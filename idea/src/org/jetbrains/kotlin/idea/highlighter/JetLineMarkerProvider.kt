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

package org.jetbrains.kotlin.idea.highlighter

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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.NullableFunction
import kotlin.KotlinPackage
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.highlighter.markers.*
import org.jetbrains.kotlin.idea.highlighter.markers.ResolveWithParentsResult
import org.jetbrains.kotlin.idea.highlighter.markers.SuperDeclarationMarkerNavigationHandler
import org.jetbrains.kotlin.idea.highlighter.markers.SuperDeclarationMarkerTooltip
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

import javax.swing.*
import java.awt.event.MouseEvent

public class JetLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        // all Kotlin markers are added in slow marker pass
        return null
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        if (elements.isEmpty()) return

        val first = KotlinPackage.first<PsiElement>(elements)
        if (DumbService.getInstance(first.getProject()).isDumb() || !ProjectRootsUtil.isInProjectOrLibSource(elements.get(0))) {
            return
        }

        val functions = Sets.newHashSet<JetNamedFunction>()
        val properties = Sets.newHashSet<JetProperty>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            if (element is JetClass) {
                collectInheritedClassMarker(element, result)
            }
            else if (element is JetNamedFunction) {

                functions.add(element)
                collectSuperDeclarationMarkers(element, result)
            }
            else if (element is JetProperty) {

                properties.add(element)
                collectSuperDeclarationMarkers(element, result)
            }
        }

        collectOverridingAccessors(functions, result)
        collectOverridingPropertiesAccessors(properties, result)
    }

    companion object {
        public val OVERRIDING_MARK: Icon = AllIcons.Gutter.OverridingMethod
        public val IMPLEMENTING_MARK: Icon = AllIcons.Gutter.ImplementingMethod
        protected val OVERRIDDEN_MARK: Icon = AllIcons.Gutter.OverridenMethod
        protected val IMPLEMENTED_MARK: Icon = AllIcons.Gutter.ImplementedMethod

        private val SUBCLASSED_CLASS = MarkerType(object : NullableFunction<PsiElement, String> {
            override fun `fun`(element: PsiElement?): String? {
                val psiClass = getPsiClass(element)
                return if (psiClass != null) MarkerType.getSubclassedClassTooltip(psiClass) else null
            }
        },
                                                  object : LineMarkerNavigator() {
                                                      override fun browse(e: MouseEvent?, element: PsiElement?) {
                                                          val psiClass = getPsiClass(element)
                                                          if (psiClass != null) {
                                                              MarkerType.navigateToSubclassedClass(e, psiClass)
                                                          }
                                                      }
                                                  })

        private val OVERRIDDEN_FUNCTION = MarkerType(object : NullableFunction<PsiElement, String> {
            override fun `fun`(element: PsiElement?): String? {
                val psiMethod = getPsiMethod(element)
                return if (psiMethod != null) getOverriddenMethodTooltip(psiMethod) else null
            }
        },
                                                     object : LineMarkerNavigator() {
                                                         override fun browse(e: MouseEvent?, element: PsiElement?) {
                                                             val psiMethod = getPsiMethod(element)
                                                             if (psiMethod != null) {
                                                                 navigateToOverriddenMethod(e, psiMethod)
                                                             }
                                                         }
                                                     })

        private val OVERRIDDEN_PROPERTY = MarkerType(object : NullableFunction<PsiElement, String> {
            override fun `fun`(element: PsiElement?): String? {
                if (element == null) return null

                assert(element.getParent() is JetProperty, "This tooltip provider should be placed only on identifies in properties")
                val property = element.getParent() as JetProperty

                return getOverriddenPropertyTooltip(property)
            }
        },
                                                     object : LineMarkerNavigator() {
                                                         override fun browse(e: MouseEvent?, element: PsiElement?) {
                                                             if (element == null) return

                                                             assert(element.getParent() is JetProperty, "This marker navigator should be placed only on identifies in properties")
                                                             val property = element.getParent() as JetProperty

                                                             navigateToPropertyOverriddenDeclarations(e, property)
                                                         }
                                                     })

        private fun isImplementsAndNotOverrides(descriptor: CallableMemberDescriptor, overriddenMembers: Collection<CallableMemberDescriptor>): Boolean {
            if (descriptor.getModality() == Modality.ABSTRACT) return false

            for (function in overriddenMembers) {
                if (function.getModality() != Modality.ABSTRACT) return false
            }

            return true
        }

        private fun collectSuperDeclarationMarkers(declaration: JetDeclaration, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
            assert((declaration is JetNamedFunction || declaration is JetProperty))

            if (!declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return

            val resolveWithParents = resolveDeclarationWithParents(declaration)
            if (resolveWithParents.overriddenDescriptors.isEmpty()) return

            // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
            // clearing the whole BindingTrace.
            val marker = LineMarkerInfo(declaration, declaration.getTextOffset(), if (isImplementsAndNotOverrides(resolveWithParents.descriptor, resolveWithParents.overriddenDescriptors))
                IMPLEMENTING_MARK
            else
                OVERRIDING_MARK, Pass.UPDATE_OVERRIDEN_MARKERS, SuperDeclarationMarkerTooltip, SuperDeclarationMarkerNavigationHandler())

            result.add(marker)
        }

        private fun collectInheritedClassMarker(element: JetClass, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
            val isTrait = element.isTrait()
            if (!(isTrait || element.hasModifier(JetTokens.OPEN_KEYWORD) || element.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
                return
            }

            val lightClass = LightClassUtil.getPsiClass(element)
            if (lightClass == null) return

            val inheritor = ClassInheritorsSearch.search(lightClass, false).findFirst()
            if (inheritor != null) {
                val nameIdentifier = element.getNameIdentifier()
                val anchor = nameIdentifier ?: element
                val mark = if (isTrait) IMPLEMENTED_MARK else OVERRIDDEN_MARK
                result.add(LineMarkerInfo(anchor, anchor.getTextOffset(), mark, Pass.UPDATE_OVERRIDEN_MARKERS, SUBCLASSED_CLASS.getTooltip(), SUBCLASSED_CLASS.getNavigationHandler()))
            }
        }

        public fun isImplemented(declaration: JetNamedDeclaration): Boolean {
            if (declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return true

            var parent = declaration.getParent()
            parent = if ((parent is JetClassBody)) parent.getParent() else parent

            if (parent is JetClass) {
                return (parent as JetClass).isTrait() && (declaration !is JetDeclarationWithBody || !declaration.hasBody()) && (declaration !is JetWithExpressionInitializer || !declaration.hasInitializer())
            }

            return false
        }

        private fun collectOverridingPropertiesAccessors(properties: Collection<JetProperty>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
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

                val info = LineMarkerInfo(anchor, anchor!!.getTextOffset(), if (isImplemented(property)) IMPLEMENTED_MARK else OVERRIDDEN_MARK, Pass.UPDATE_OVERRIDEN_MARKERS, OVERRIDDEN_PROPERTY.getTooltip(), OVERRIDDEN_PROPERTY.getNavigationHandler(), GutterIconRenderer.Alignment.RIGHT)

                result.add(info)
            }
        }

        private fun collectOverridingAccessors(functions: Collection<JetNamedFunction>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
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

                var anchor = function.getNameIdentifier()
                if (anchor == null) anchor = function

                val info = LineMarkerInfo(anchor, anchor!!.getTextOffset(), if (isImplemented(function)) IMPLEMENTED_MARK else OVERRIDDEN_MARK, Pass.UPDATE_OVERRIDEN_MARKERS, OVERRIDDEN_FUNCTION.getTooltip(), OVERRIDDEN_FUNCTION.getNavigationHandler(), GutterIconRenderer.Alignment.RIGHT)

                result.add(info)
            }
        }
    }
}
