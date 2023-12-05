/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.RenderIrElementVisitorForDiagnosticText
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

internal object IrActualizationErrors {
    val NO_ACTUAL_FOR_EXPECT by error2<PsiElement, String, ModuleDescriptor>()
    val AMBIGUOUS_ACTUALS by error2<PsiElement, String, ModuleDescriptor>()
    val EXPECT_ACTUAL_MISMATCH by error3<PsiElement, String, String, ExpectActualMatchingCompatibility.Mismatch>()
    val EXPECT_ACTUAL_INCOMPATIBILITY by error3<PsiElement, String, String, ExpectActualCheckingCompatibility.Incompatible<*>>()
    val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT by warning3<PsiElement, IrSymbol, IrSymbol, ExpectActualAnnotationsIncompatibilityType<IrConstructorCall>>()
    val ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE by error1<PsiElement, IrValueParameter>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultIrActualizationErrorMessages)
    }
}

internal object KtDefaultIrActualizationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            IrActualizationErrors.AMBIGUOUS_ACTUALS,
            "{0} has several compatible actual declarations in modules {1}",
            CommonRenderers.STRING,
            IrActualizationDiagnosticRenderers.MODULE_WITH_PLATFORM,
        )
        map.put(
            IrActualizationErrors.NO_ACTUAL_FOR_EXPECT,
            "Expected {0} has no actual declaration in module {1}",
            CommonRenderers.STRING,
            IrActualizationDiagnosticRenderers.MODULE_WITH_PLATFORM,
        )
        map.put(
            IrActualizationErrors.EXPECT_ACTUAL_MISMATCH,
            "Expect declaration `{0}` doesn''t match actual `{1}` because {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            IrActualizationDiagnosticRenderers.MISMATCH
        )
        map.put(
            IrActualizationErrors.EXPECT_ACTUAL_INCOMPATIBILITY,
            "Expect declaration `{0}` is incompatible with actual `{1}` because {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            IrActualizationDiagnosticRenderers.INCOMPATIBILITY
        )
        map.put(
            IrActualizationErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
            "{2}.\n" +
                    "All annotations from expect `{0}` must be present with the same arguments on actual `{1}`, otherwise they might behave incorrectly.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
            IrActualizationDiagnosticRenderers.EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY,
        )

        map.put(
            IrActualizationErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE,
            "Parameter ''{0}'' has conflicting values in expected and actual annotations.",
            IrDiagnosticRenderers.DECLARATION_NAME,
        )
    }
}

internal object IrActualizationDiagnosticRenderers {
    val MISMATCH = Renderer<ExpectActualMatchingCompatibility.Mismatch> {
        it.reason ?: "<unknown>"
    }
    val INCOMPATIBILITY = Renderer<ExpectActualCheckingCompatibility.Incompatible<*>> {
        it.reason ?: "<unknown>"
    }
    val EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY =
        Renderer { incompatibilityType: ExpectActualAnnotationsIncompatibilityType<IrConstructorCall> ->
            val expectAnnotation = ANNOTATION.render(incompatibilityType.expectAnnotation)
            val reason = when (incompatibilityType) {
                is ExpectActualAnnotationsIncompatibilityType.MissingOnActual -> "is missing on actual declaration"
                is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual -> {
                    val actualAnnotation = ANNOTATION.render(incompatibilityType.actualAnnotation)
                    "has different arguments on actual declaration: `$actualAnnotation`"
                }
            }
            "Annotation `$expectAnnotation` $reason"
        }

    private val ANNOTATION = Renderer<IrConstructorCall> {
        "@" + RenderIrElementVisitorForDiagnosticText.renderAsAnnotation(it)
    }

    @JvmField
    val MODULE_WITH_PLATFORM = Renderer<ModuleDescriptor> { module ->
        val platform = module.platform
        val moduleName = module.getCapability(ModuleInfo.Capability)?.displayedName ?: module.name.asString()
        val platformNameIfAny = if (platform == null || platform.isCommon()) "" else " for " + platform.single().platformName
        moduleName + platformNameIfAny
    }
}
