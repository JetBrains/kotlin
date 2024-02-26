/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.StubTypeForBuilderInference

object StubForBuilderInferenceParameterTypeChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtParameter ||
            descriptor !is ValueDescriptor ||
            descriptor.returnType !is StubTypeForBuilderInference
        ) return
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.NoBuilderInferenceWithoutAnnotationRestriction)) return

        context.trace.report(Errors.BUILDER_INFERENCE_STUB_PARAMETER_TYPE.on(declaration, declaration.nameAsSafeName))
    }
}
