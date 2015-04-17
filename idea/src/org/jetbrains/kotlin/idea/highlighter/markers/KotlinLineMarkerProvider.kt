/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter.markers

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator
import com.intellij.codeInsight.daemon.impl.MarkerType
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.NullableFunction
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isOverridable
import java.awt.event.MouseEvent
import java.util.HashSet
import javax.swing.Icon

public class KotlinLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        // all Kotlin markers are added in slow marker pass
        return null
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<*>>) {
        if (elements.isEmpty()) return

        val first = elements.first()
        if (DumbService.getInstance(first.getProject()).isDumb() || !ProjectRootsUtil.isInProjectOrLibSource(first)) return

        val functions = HashSet<JetNamedFunction>()
        val properties = HashSet<JetProperty>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            when (element) {
                is JetClass -> {
                    collectInheritedClassMarker(element, result)
                }
                is JetNamedFunction -> {
                    functions.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }
                is JetProperty -> {
                    properties.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }
            }
        }

        collectOverridingAccessors(functions, result)
        collectOverridingPropertiesAccessors(properties, result)
    }
}

private val OVERRIDING_MARK: Icon = AllIcons.Gutter.OverridingMethod
private val IMPLEMENTING_MARK: Icon = AllIcons.Gutter.ImplementingMethod
private val OVERRIDDEN_MARK: Icon = AllIcons.Gutter.OverridenMethod
private val IMPLEMENTED_MARK: Icon = AllIcons.Gutter.ImplementedMethod

private val SUBCLASSED_CLASS = MarkerType(
        { getPsiClass(it)?.let { MarkerType.getSubclassedClassTooltip(it) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                getPsiClass(element)?.let { MarkerType.navigateToSubclassedClass(e, it) }
            }
        })

private val OVERRIDDEN_FUNCTION = MarkerType(
        { getPsiMethod(it)?.let { getOverriddenMethodTooltip(it) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                getPsiMethod(element)?.let { navigateToOverriddenMethod(e, it) }
            }
        })

private val OVERRIDDEN_PROPERTY = MarkerType(
        { it?.let { getOverriddenPropertyTooltip(it.getParent() as JetProperty) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                element?.let { navigateToPropertyOverriddenDeclarations(e, it.getParent() as JetProperty) }
            }
        })

private fun isImplementsAndNotOverrides(descriptor: CallableMemberDescriptor, overriddenMembers: Collection<CallableMemberDescriptor>): Boolean {
    return descriptor.getModality() != Modality.ABSTRACT && overriddenMembers.all { it.getModality() == Modality.ABSTRACT }
}

private fun collectSuperDeclarationMarkers(declaration: JetDeclaration, result: MutableCollection<LineMarkerInfo<*>>) {
    assert((declaration is JetNamedFunction || declaration is JetProperty))

    if (!declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return

    val resolveWithParents = resolveDeclarationWithParents(declaration)
    if (resolveWithParents.overriddenDescriptors.isEmpty()) return

    val implements = isImplementsAndNotOverrides(resolveWithParents.descriptor!!, resolveWithParents.overriddenDescriptors)

    // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
    // clearing the whole BindingTrace.
    val marker = LineMarkerInfo(
            declaration,
            declaration.getTextOffset(),
            if (implements) IMPLEMENTING_MARK else OVERRIDING_MARK,
            Pass.UPDATE_OVERRIDEN_MARKERS,
            SuperDeclarationMarkerTooltip,
            SuperDeclarationMarkerNavigationHandler()
    )

    result.add(marker)
}

private fun collectInheritedClassMarker(element: JetClass, result: MutableCollection<LineMarkerInfo<*>>) {
    val isTrait = element.isTrait()
    if (!(isTrait || element.hasModifier(JetTokens.OPEN_KEYWORD) || element.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
        return
    }

    val lightClass = LightClassUtil.getPsiClass(element) ?: return

    if (ClassInheritorsSearch.search(lightClass, false).findFirst() == null) return

    val anchor = element.getNameIdentifier() ?: element

    result.add(LineMarkerInfo(
            anchor,
            anchor.getTextOffset(),
            if (isTrait) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
            Pass.UPDATE_OVERRIDEN_MARKERS,
            SUBCLASSED_CLASS.getTooltip(),
            SUBCLASSED_CLASS.getNavigationHandler()
    ))
}

private fun collectOverridingPropertiesAccessors(properties: Collection<JetProperty>, result: MutableCollection<LineMarkerInfo<*>>) {
    val mappingToJava = Maps.newHashMap<PsiMethod, JetProperty>()
    for (property in properties) {
        if (property.isOverridable()) {
            val accessorsPsiMethods = LightClassUtil.getLightClassPropertyMethods(property)
            for (psiMethod in accessorsPsiMethods) {
                mappingToJava.put(psiMethod, property)
            }
        }
    }

    val classes = collectContainingClasses(mappingToJava.keySet())

    for (property in getOverriddenDeclarations(mappingToJava, classes)) {
        ProgressManager.checkCanceled()

        var anchor = property.getNameIdentifier()
        if (anchor == null) anchor = property

        result.add(LineMarkerInfo(
                anchor,
                anchor!!.getTextOffset(),
                if (isImplemented(property)) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
                Pass.UPDATE_OVERRIDEN_MARKERS,
                OVERRIDDEN_PROPERTY.getTooltip(),
                OVERRIDDEN_PROPERTY.getNavigationHandler(),
                GutterIconRenderer.Alignment.RIGHT
        ))
    }
}

private fun collectOverridingAccessors(functions: Collection<JetNamedFunction>, result: MutableCollection<LineMarkerInfo<*>>) {
    val mappingToJava = Maps.newHashMap<PsiMethod, JetNamedFunction>()
    for (function in functions) {
        if (function.isOverridable()) {
            val method = LightClassUtil.getLightClassMethod(function)
            if (method != null) {
                mappingToJava.put(method, function)
            }
        }
    }

    val classes = collectContainingClasses(mappingToJava.keySet())

    for (function in getOverriddenDeclarations(mappingToJava, classes)) {
        ProgressManager.checkCanceled()

        val anchor = function.getNameIdentifier() ?: function

        result.add(LineMarkerInfo(
                anchor,
                anchor!!.getTextOffset(),
                if (isImplemented(function)) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
                Pass.UPDATE_OVERRIDEN_MARKERS, OVERRIDDEN_FUNCTION.getTooltip(),
                OVERRIDDEN_FUNCTION.getNavigationHandler(),
                GutterIconRenderer.Alignment.RIGHT
        ))
    }
}
