/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.config.LanguageFeature
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

internal fun PropertyDescriptor.propertyKind(usageModule: ModuleDescriptor?): DataFlowValue.Kind {
    if (isVar) return DataFlowValue.Kind.MUTABLE_PROPERTY
    if (isOverridable) return DataFlowValue.Kind.PROPERTY_WITH_GETTER
    if (!hasDefaultGetter()) return DataFlowValue.Kind.PROPERTY_WITH_GETTER
    if (!isInvisibleFromOtherModules()) {
        val declarationModule = DescriptorUtils.getContainingModule(this)
        if (usageModule == null || usageModule != declarationModule) {
            return DataFlowValue.Kind.ALIEN_PUBLIC_PROPERTY
        }
    }
    return DataFlowValue.Kind.STABLE_VALUE
}

internal fun VariableDescriptor.variableKind(
    usageModule: ModuleDescriptor?,
    bindingContext: BindingContext,
    accessElement: KtElement,
    languageVersionSettings: LanguageVersionSettings
): DataFlowValue.Kind {
    if (this is PropertyDescriptor) {
        return propertyKind(usageModule)
    }

    if (this is LocalVariableDescriptor && this.isDelegated) {
        // Local delegated property: normally unstable, but can be treated as stable in legacy mode
        return if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSmartcastsOnLocalDelegatedProperty))
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
        return if (preliminaryVisitor.languageVersionSettings.supportsFeature(LanguageFeature.CapturedInClosureSmartCasts) &&
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
        val writerDescriptor = writerDeclaration?.let {
            ControlFlowInformationProvider.getDeclarationDescriptorIncludingConstructors(bindingContext, it)
        }
        writerDeclaration != null && variableContainingDeclaration != writerDescriptor
    }
}

private fun isAccessedInsideClosureAfterAllWriters(
    writers: Set<AssignedVariablesSearcher.Writer>,
    accessElement: KtElement
): Boolean {
    val parent = ControlFlowInformationProvider.getElementParentDeclaration(accessElement) ?: return false
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
        val writerDescriptor = ControlFlowInformationProvider.getDeclarationDescriptorIncludingConstructors(
            bindingContext, writerDeclaration
        )
        // Access is after some writerDeclaration
        if (variableContainingDeclaration != writerDescriptor && !accessElement.before(writerDeclaration)) {
            return false
        }
    }
    // Access is before all writers
    return true
}


private fun DeclarationDescriptorWithVisibility.isInvisibleFromOtherModules(): Boolean {
    if (Visibilities.INVISIBLE_FROM_OTHER_MODULES.contains(visibility)) return true

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
    val parent = ControlFlowInformationProvider.getElementParentDeclaration(accessElement)
    return if (parent != null) // Access is at the same declaration: not in closure, lower: in closure
        ControlFlowInformationProvider.getDeclarationDescriptorIncludingConstructors(bindingContext, parent) !=
                variableContainingDeclaration
    else
        false
}
