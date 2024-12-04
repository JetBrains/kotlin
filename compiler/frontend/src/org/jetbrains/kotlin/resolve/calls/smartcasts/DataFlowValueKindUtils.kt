/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.cfg.getDeclarationDescriptorIncludingConstructors
import org.jetbrains.kotlin.cfg.getElementParentDeclaration
import org.jetbrains.kotlin.config.LanguageFeature.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.before
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.expressions.AssignedVariablesSearcher
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor

internal fun PropertyDescriptor.propertyKind(
    usageModule: ModuleDescriptor?,
    languageVersionSettings: LanguageVersionSettings
): DataFlowValue.Kind {
    if (isVar) return DataFlowValue.Kind.MUTABLE_PROPERTY
    if (isOverridable) return DataFlowValue.Kind.PROPERTY_WITH_GETTER
    if (!hasDefaultGetter()) return DataFlowValue.Kind.PROPERTY_WITH_GETTER
    val originalDescriptor = DescriptorUtils.unwrapFakeOverride(this)
    val isInvisibleFromOtherModuleUnwrappedFakeOverride = originalDescriptor.isInvisibleFromOtherModules()
    if (isInvisibleFromOtherModuleUnwrappedFakeOverride) return DataFlowValue.Kind.STABLE_VALUE

    if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        if (overriddenDescriptors.any { isDeclaredInAnotherModule(usageModule) }) {
            val deprecationForInvisibleFakeOverride =
                isInvisibleFromOtherModules() != isInvisibleFromOtherModuleUnwrappedFakeOverride &&
                        !languageVersionSettings.supportsFeature(ProhibitSmartcastsOnPropertyFromAlienBaseClassInheritedInInvisibleClass)
            return when {
                deprecationForInvisibleFakeOverride ->
                    DataFlowValue.Kind.LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS
                !languageVersionSettings.supportsFeature(ProhibitSmartcastsOnPropertyFromAlienBaseClass) ->
                    DataFlowValue.Kind.LEGACY_ALIEN_BASE_PROPERTY
                else ->
                    DataFlowValue.Kind.ALIEN_PUBLIC_PROPERTY
            }
        }
    }

    val declarationModule = DescriptorUtils.getContainingModule(originalDescriptor)
    if (!areCompiledTogether(usageModule, declarationModule)) return DataFlowValue.Kind.ALIEN_PUBLIC_PROPERTY

    return DataFlowValue.Kind.STABLE_VALUE
}

private fun PropertyDescriptor.isDeclaredInAnotherModule(usageModule: ModuleDescriptor?): Boolean {
    if (!areCompiledTogether(usageModule, DescriptorUtils.getContainingModule(this))) {
        return true
    }
    if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return overriddenDescriptors.any { it.isDeclaredInAnotherModule(usageModule) }
    }
    return false
}

private fun areCompiledTogether(
    usageModule: ModuleDescriptor?,
    declarationModule: ModuleDescriptor,
): Boolean {
    if (usageModule == null) return false
    if (usageModule == declarationModule) return true

    return declarationModule in usageModule.allExpectedByModules
}

internal fun VariableDescriptor.variableKind(
    usageModule: ModuleDescriptor?,
    bindingContext: BindingContext,
    accessElement: KtElement,
    languageVersionSettings: LanguageVersionSettings
): DataFlowValue.Kind {
    if (this is PropertyDescriptor) {
        return propertyKind(usageModule, languageVersionSettings)
    }

    if (this is LocalVariableDescriptor && this.isDelegated) {
        // Local delegated property: normally unstable, but can be treated as stable in legacy mode
        return if (languageVersionSettings.supportsFeature(ProhibitSmartcastsOnLocalDelegatedProperty))
            DataFlowValue.Kind.PROPERTY_WITH_GETTER
        else
            DataFlowValue.Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY

    }

    if (this !is LocalVariableDescriptor && this !is ParameterDescriptor) return DataFlowValue.Kind.OTHER
    if (!isVar) return DataFlowValue.Kind.STABLE_VALUE
    if (this is SyntheticFieldDescriptor) return DataFlowValue.Kind.MUTABLE_PROPERTY

    // Local variable classification: STABLE or CAPTURED
    val preliminaryVisitor = PreliminaryDeclarationVisitor.getVisitorByVariable(this, bindingContext)
    // A case when we just analyse an expression alone: counts as captured
            ?: return DataFlowValue.Kind.CAPTURED_VARIABLE

    // Analyze who writes variable
    // If there is no writer: stable
    val writers = preliminaryVisitor.writers(this)
    if (writers.isEmpty()) return DataFlowValue.Kind.STABLE_VARIABLE

    // If access element is inside closure: captured
    val variableContainingDeclaration = this.containingDeclaration
    if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) {
        // stable iff we have no writers in closures AND this closure is AFTER all writers
        return if (preliminaryVisitor.languageVersionSettings.supportsFeature(CapturedInClosureSmartCasts) &&
            hasNoWritersInClosures(variableContainingDeclaration, writers, bindingContext) &&
            isAccessedInsideClosureAfterAllWriters(writers, accessElement)
        ) {
            DataFlowValue.Kind.STABLE_VARIABLE
        } else {
            DataFlowValue.Kind.CAPTURED_VARIABLE
        }
    }

    // Otherwise, stable iff considered position is BEFORE all writers except declarer itself
    return if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement))
        DataFlowValue.Kind.STABLE_VARIABLE
    else
        DataFlowValue.Kind.CAPTURED_VARIABLE
}


fun hasNoWritersInClosures(
    variableContainingDeclaration: DeclarationDescriptor,
    writers: Set<AssignedVariablesSearcher.Writer>,
    bindingContext: BindingContext
): Boolean {
    return writers.none { (_, writerDeclaration) ->
        writerDeclaration != null &&
                variableContainingDeclaration != writerDeclaration.getDeclarationDescriptorIncludingConstructors(bindingContext)
    }
}

private fun isAccessedInsideClosureAfterAllWriters(
    writers: Set<AssignedVariablesSearcher.Writer>,
    accessElement: KtElement
): Boolean {
    val parent = accessElement.getElementParentDeclaration() ?: return false
    return writers.none { (assignment) -> !assignment.before(parent) }
}

private fun isAccessedBeforeAllClosureWriters(
    variableContainingDeclaration: DeclarationDescriptor,
    writers: Set<AssignedVariablesSearcher.Writer>,
    bindingContext: BindingContext,
    accessElement: KtElement
): Boolean {
    // All writers should be before access element, with the exception:
    // writer which is the same with declaration site does not count
    writers.mapNotNull { it.declaration }.forEach { writerDeclaration ->
        val writerDescriptor = writerDeclaration.getDeclarationDescriptorIncludingConstructors(bindingContext)
        // Access is after some writerDeclaration
        if (variableContainingDeclaration != writerDescriptor && !accessElement.before(writerDeclaration)) {
            return false
        }
    }
    // Access is before all writers
    return true
}

private fun DeclarationDescriptorWithVisibility.isInvisibleFromOtherModules(): Boolean {
    if (DescriptorVisibilities.INVISIBLE_FROM_OTHER_MODULES.contains(visibility)) return true

    val containingDeclaration = containingDeclaration
    return containingDeclaration is DeclarationDescriptorWithVisibility && containingDeclaration.isInvisibleFromOtherModules()
}

private fun PropertyDescriptor.hasDefaultGetter(): Boolean {
    val getter = getter
    return getter == null || getter.isDefault
}

private fun isAccessedInsideClosure(
    variableContainingDeclaration: DeclarationDescriptor,
    bindingContext: BindingContext,
    accessElement: KtElement
): Boolean {
    val parent = accessElement.getElementParentDeclaration()
    return if (parent != null) // Access is at the same declaration: not in closure, lower: in closure
        parent.getDeclarationDescriptorIncludingConstructors(bindingContext) != variableContainingDeclaration
    else
        false
}
