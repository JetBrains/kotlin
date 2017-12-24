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

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator
import com.intellij.codeInsight.daemon.impl.MarkerType
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.codeInsight.navigation.ListBackgroundUpdaterTask
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.facet.implementedDescriptor
import org.jetbrains.kotlin.idea.facet.implementingDescriptors
import org.jetbrains.kotlin.idea.search.declarationsSearch.toPossiblyFakeLightMethods
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon
import javax.swing.ListCellRenderer

class KotlinLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        // all Kotlin markers are added in slow marker pass
        return null
    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<*>>) {
        if (elements.isEmpty()) return

        val first = elements.first()
        if (DumbService.getInstance(first.project).isDumb || !ProjectRootsUtil.isInProjectOrLibSource(first)) return

        val functions = HashSet<KtNamedFunction>()
        val properties = HashSet<KtNamedDeclaration>()

        for (element in elements) {
            ProgressManager.checkCanceled()

            when (element) {
                is KtClass -> {
                    collectInheritedClassMarker(element, result)
                }
                is KtNamedFunction -> {
                    functions.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }
                is KtProperty -> {
                    properties.add(element)
                    collectSuperDeclarationMarkers(element, result)
                }
                is KtParameter -> {
                    if (element.hasValOrVar()) {
                        properties.add(element)
                        collectSuperDeclarationMarkers(element, result)
                    }
                }
            }
        }

        collectOverriddenFunctions(functions, result)
        collectOverriddenPropertyAccessors(properties, result)

        for (element in elements) {
            if (element !is KtNamedDeclaration) continue

            if (element.isExpectDeclaration()) {
                collectActualMarkers(element, result)
            }
            else if (element.isActualDeclaration()) {
                collectExpectedMarkers(element, result)
            }
        }
    }
}

internal fun KtNamedDeclaration.isExpectDeclaration(): Boolean =
        (toDescriptor() as? MemberDescriptor)?.isExpect == true

internal fun KtNamedDeclaration.isActualDeclaration(): Boolean =
        (toDescriptor() as? MemberDescriptor)?.isActual == true

private val OVERRIDING_MARK: Icon = AllIcons.Gutter.OverridingMethod
private val IMPLEMENTING_MARK: Icon = AllIcons.Gutter.ImplementingMethod
private val OVERRIDDEN_MARK: Icon = AllIcons.Gutter.OverridenMethod
private val IMPLEMENTED_MARK: Icon = AllIcons.Gutter.ImplementedMethod

data class NavigationPopupDescriptor(
        val targets: Collection<NavigatablePsiElement>,
        val title: String,
        val findUsagesTitle: String,
        val renderer: ListCellRenderer<*>,
        val updater: ListBackgroundUpdaterTask? = null
) {
    fun showPopup(e: MouseEvent?) {
        PsiElementListNavigator.openTargets(e, targets.toTypedArray(), title, findUsagesTitle, renderer, updater)
    }
}

interface TestableLineMarkerNavigator {
    fun getTargetsPopupDescriptor(element: PsiElement?): NavigationPopupDescriptor?
}

private val SUBCLASSED_CLASS = MarkerType(
        "SUBCLASSED_CLASS",
        { getPsiClass(it)?.let { MarkerType.getSubclassedClassTooltip(it) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                getPsiClass(element)?.let { MarkerType.navigateToSubclassedClass(e, it) }
            }
        })

private val OVERRIDDEN_FUNCTION = object : MarkerType(
        "OVERRIDDEN_FUNCTION",
        { getPsiMethod(it)?.let(::getOverriddenMethodTooltip) },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                buildNavigateToOverriddenMethodPopup(e, element)?.showPopup(e)
            }
        }) {

    override fun getNavigationHandler(): GutterIconNavigationHandler<PsiElement> {
        val superHandler = super.getNavigationHandler()
        return object : GutterIconNavigationHandler<PsiElement>, TestableLineMarkerNavigator {
            override fun navigate(e: MouseEvent?, elt: PsiElement?) {
                superHandler.navigate(e, elt)
            }

            override fun getTargetsPopupDescriptor(element: PsiElement?) = buildNavigateToOverriddenMethodPopup(null, element)
        }
    }
}

private val OVERRIDDEN_PROPERTY = object : MarkerType(
        "OVERRIDDEN_PROPERTY",
        { it?.let { getOverriddenPropertyTooltip(it.parent as KtNamedDeclaration) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                buildNavigateToPropertyOverriddenDeclarationsPopup(e, element)?.showPopup(e)
            }
        }) {

    override fun getNavigationHandler(): GutterIconNavigationHandler<PsiElement> {
        val superHandler = super.getNavigationHandler()
        return object : GutterIconNavigationHandler<PsiElement>, TestableLineMarkerNavigator {
            override fun navigate(e: MouseEvent?, elt: PsiElement?) {
                superHandler.navigate(e, elt)
            }

            override fun getTargetsPopupDescriptor(element: PsiElement?) = buildNavigateToPropertyOverriddenDeclarationsPopup(null, element)
        }
    }
}

private val PsiElement.markerDeclaration
    get() = (this as? KtDeclaration) ?: (parent as? KtDeclaration)

private val PLATFORM_ACTUAL = MarkerType(
        "PLATFORM_ACTUAL",
        { it?.let { getPlatformActualTooltip(it.markerDeclaration) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                element?.let { navigateToPlatformActual(e, it.markerDeclaration) }
            }
        }
)

private val EXPECTED_DECLARATION = MarkerType(
        "EXPECTED_DECLARATION",
        { it?.let { getExpectedDeclarationTooltip(it.markerDeclaration) } },
        object : LineMarkerNavigator() {
            override fun browse(e: MouseEvent?, element: PsiElement?) {
                element?.let { navigateToExpectedDeclaration(it.markerDeclaration) }
            }
        }
)

private fun isImplementsAndNotOverrides(descriptor: CallableMemberDescriptor, overriddenMembers: Collection<CallableMemberDescriptor>): Boolean {
    return descriptor.modality != Modality.ABSTRACT && overriddenMembers.all { it.modality == Modality.ABSTRACT }
}

private fun collectSuperDeclarationMarkers(declaration: KtDeclaration, result: MutableCollection<LineMarkerInfo<*>>) {
    assert(declaration is KtNamedFunction || declaration is KtProperty || declaration is KtParameter)

    if (!declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return

    val resolveWithParents = resolveDeclarationWithParents(declaration)
    if (resolveWithParents.overriddenDescriptors.isEmpty()) return

    val implements = isImplementsAndNotOverrides(resolveWithParents.descriptor!!, resolveWithParents.overriddenDescriptors)

    val anchor = (declaration as? KtNamedDeclaration)?.nameIdentifier ?: declaration

    // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
    // clearing the whole BindingTrace.

    result.add(LineMarkerInfo(
            anchor,
            anchor.textRange,
            if (implements) IMPLEMENTING_MARK else OVERRIDING_MARK,
            Pass.LINE_MARKERS,
            SuperDeclarationMarkerTooltip,
            SuperDeclarationMarkerNavigationHandler(),
            GutterIconRenderer.Alignment.RIGHT
    ))
}

private fun collectInheritedClassMarker(element: KtClass, result: MutableCollection<LineMarkerInfo<*>>) {
    if (!element.isInheritable()) {
        return
    }

    val lightClass = element.toLightClass() ?: KtFakeLightClass(element)

    if (ClassInheritorsSearch.search(lightClass, false).findFirst() == null) return

    val anchor = element.nameIdentifier ?: element

    result.add(LineMarkerInfo(
            anchor,
            anchor.textRange,
            if (element.isInterface()) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
            Pass.LINE_MARKERS,
            SUBCLASSED_CLASS.tooltip,
            SUBCLASSED_CLASS.navigationHandler,
            GutterIconRenderer.Alignment.RIGHT
    ))
}

private fun collectOverriddenPropertyAccessors(properties: Collection<KtNamedDeclaration>,
                                               result: MutableCollection<LineMarkerInfo<*>>) {
    val mappingToJava = HashMap<PsiElement, KtNamedDeclaration>()
    for (property in properties) {
        if (property.isOverridable()) {
            property.toPossiblyFakeLightMethods().forEach { mappingToJava.put(it, property) }
            mappingToJava[property] = property
        }
    }

    val classes = collectContainingClasses(mappingToJava.keys.filterIsInstance<PsiMethod>())

    for (property in getOverriddenDeclarations(mappingToJava, classes)) {
        ProgressManager.checkCanceled()

        val anchor = (property as? PsiNameIdentifierOwner)?.nameIdentifier ?: property

        result.add(LineMarkerInfo(
                anchor,
                anchor.textRange,
                if (isImplemented(property)) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
                Pass.LINE_MARKERS,
                OVERRIDDEN_PROPERTY.tooltip,
                OVERRIDDEN_PROPERTY.navigationHandler,
                GutterIconRenderer.Alignment.RIGHT
        ))
    }
}

private val KtNamedDeclaration.expectOrActualAnchor
    get() =
        nameIdentifier
        ?: (this as? KtConstructor<*>)?.let {
            it.getConstructorKeyword() ?: it.getValueParameterList()?.leftParenthesis
        }
        ?: this

private fun collectActualMarkers(declaration: KtNamedDeclaration,
                                 result: MutableCollection<LineMarkerInfo<*>>) {

    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return
    val commonModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    if (commonModuleDescriptor.implementingDescriptors.none { it.hasActualsFor(descriptor) }) return

    val anchor = declaration.expectOrActualAnchor

    result.add(LineMarkerInfo(
            anchor,
            anchor.textRange,
            KotlinIcons.FROM_EXPECTED,
            Pass.LINE_MARKERS,
            PLATFORM_ACTUAL.tooltip,
            PLATFORM_ACTUAL.navigationHandler,
            GutterIconRenderer.Alignment.RIGHT
    ))
}

private fun collectExpectedMarkers(declaration: KtNamedDeclaration,
                                   result: MutableCollection<LineMarkerInfo<*>>) {

    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return
    val platformModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()
    val commonModuleDescriptor = platformModuleDescriptor.implementedDescriptor ?: return
    if (!commonModuleDescriptor.hasDeclarationOf(descriptor)) return

    val anchor = declaration.expectOrActualAnchor

    result.add(LineMarkerInfo(
            anchor,
            anchor.textRange,
            KotlinIcons.FROM_ACTUAL,
            Pass.LINE_MARKERS,
            EXPECTED_DECLARATION.tooltip,
            EXPECTED_DECLARATION.navigationHandler,
            GutterIconRenderer.Alignment.RIGHT
    ))
}

private fun collectOverriddenFunctions(functions: Collection<KtNamedFunction>, result: MutableCollection<LineMarkerInfo<*>>) {
    val mappingToJava = HashMap<PsiElement, KtNamedFunction>()
    for (function in functions) {
        if (function.isOverridable()) {
            val method = LightClassUtil.getLightClassMethod(function) ?: KtFakeLightMethod.get(function)
            if (method != null) {
                mappingToJava.put(method, function)
            }
            mappingToJava.put(function, function)
        }
    }

    val classes = collectContainingClasses(mappingToJava.keys.filterIsInstance<PsiMethod>())

    for (function in getOverriddenDeclarations(mappingToJava, classes)) {
        ProgressManager.checkCanceled()

        val anchor = function.nameIdentifier ?: function

        result.add(LineMarkerInfo(
                anchor,
                anchor.textRange,
                if (isImplemented(function)) IMPLEMENTED_MARK else OVERRIDDEN_MARK,
                Pass.LINE_MARKERS, OVERRIDDEN_FUNCTION.tooltip,
                OVERRIDDEN_FUNCTION.navigationHandler,
                GutterIconRenderer.Alignment.RIGHT
        ))
    }
}
