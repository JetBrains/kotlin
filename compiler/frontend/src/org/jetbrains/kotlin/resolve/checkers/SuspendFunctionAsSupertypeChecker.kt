/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

object SuspendFunctionAsSupertypeChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)) return
        if (descriptor !is ClassDescriptor) return

        val functionalSupertypes = descriptor.getAllSuperClassifiers().filterIsInstance<FunctionClassDescriptor>().toList()

        if (functionalSupertypes.none {
                it.functionTypeKind == FunctionTypeKind.SuspendFunction ||
                        it.functionTypeKind == FunctionTypeKind.KSuspendFunction
            }
        ) return

        if (functionalSupertypes.any {
                it.functionTypeKind == FunctionTypeKind.Function ||
                        it.functionTypeKind == FunctionTypeKind.KFunction
            }
        ) {
            val reportOn = (declaration as? KtClassOrObject)?.getSuperTypeList() ?: declaration
            context.trace.report(Errors.MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES.on(reportOn))
        }
    }
}

