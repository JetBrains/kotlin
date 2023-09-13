/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.BackendDiagnosticRenderers.EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY
import org.jetbrains.kotlin.backend.common.BackendDiagnosticRenderers.INCOMPATIBILITY
import org.jetbrains.kotlin.backend.common.BackendDiagnosticRenderers.DECLARATION_NAME
import org.jetbrains.kotlin.backend.common.BackendDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.MODULE_WITH_PLATFORM
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

object CommonBackendErrors {
    val NO_ACTUAL_FOR_EXPECT by error2<PsiElement, String, ModuleDescriptor>()
    val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by error2<PsiElement, String, String>()
    val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by error2<PsiElement, String, String>()
    val INCOMPATIBLE_MATCHING by error3<PsiElement, String, String, ExpectActualCompatibility.Incompatible<*>>()
    val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT by warning3<PsiElement, IrSymbol, IrSymbol, ExpectActualAnnotationsIncompatibilityType<IrConstructorCall>>()
    val EVALUATION_ERROR by error1<PsiElement, String>()
    val ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE by error1<PsiElement, IrValueParameter>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultCommonBackendErrorMessages)
    }
}

object KtDefaultCommonBackendErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            CommonBackendErrors.NO_ACTUAL_FOR_EXPECT,
            "Expected {0} has no actual declaration in module {1}",
            STRING,
            MODULE_WITH_PLATFORM,
        )
        map.put(
            CommonBackendErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED,
            "{0} must override {1} because it inherits multiple interface methods of it",
            STRING,
            STRING,
        )
        map.put(
            CommonBackendErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED,
            "{0} must override {1} because it inherits many implementations of it",
            STRING,
            STRING,
        )
        map.put(
            CommonBackendErrors.INCOMPATIBLE_MATCHING,
            "Expect declaration `{0}` doesn''t match actual `{1}` because {2}",
            STRING,
            STRING,
            INCOMPATIBILITY
        )
        map.put(
            CommonBackendErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
            "{2}.\n" +
                    "All annotations from expect `{0}` must be present with the same arguments on actual `{1}`, otherwise they might behave incorrectly.",
            SYMBOL_OWNER_DECLARATION_FQ_NAME,
            SYMBOL_OWNER_DECLARATION_FQ_NAME,
            EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY,
        )
        map.put(
            CommonBackendErrors.EVALUATION_ERROR,
            "Cannot evaluate constant expression: {0}",
            STRING,
        )
        map.put(
            CommonBackendErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE,
            "Parameter ''{0}'' has conflicting values in expected and actual annotations.",
            DECLARATION_NAME,
        )
    }
}

object BackendDiagnosticRenderers {
    val INCOMPATIBILITY = Renderer<ExpectActualCompatibility.Incompatible<*>> {
        it.reason ?: "<unknown>"
    }
    val SYMBOL_OWNER_DECLARATION_FQ_NAME = Renderer<IrSymbol> {
        (it.owner as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString() ?: "unknown name"
    }
    val EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY =
        Renderer { incompatibilityType: ExpectActualAnnotationsIncompatibilityType<IrConstructorCall> ->
            val expectAnnotationFqName = incompatibilityType.expectAnnotation.type.classFqName ?: "<unknown>"
            val reason = when (incompatibilityType) {
                is ExpectActualAnnotationsIncompatibilityType.MissingOnActual -> "is missing on actual declaration"
                is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual -> "has different arguments on actual declaration"
            }
            "Annotation `$expectAnnotationFqName` $reason"
        }
    val DECLARATION_NAME = Renderer<IrDeclarationWithName> { it.name.asString() }
}
