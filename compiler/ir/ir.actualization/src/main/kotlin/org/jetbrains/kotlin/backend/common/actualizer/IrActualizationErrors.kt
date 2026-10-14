/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.RenderIrElementVisitor
import org.jetbrains.kotlin.ir.util.fqNameWithoutFileClassesWhenAvailable
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.ir.util.moduleFragment
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

object IrActualizationErrors : KtDiagnosticsContainer() {
    val NO_ACTUAL_FOR_EXPECT by error2<PsiElement, IrSymbolWithModule, ModuleInfoForDiagnostic>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val AMBIGUOUS_ACTUALS by error2<PsiElement, IrSymbolWithModule, Set<ModuleInfoForDiagnostic>>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val EXPECT_ACTUAL_IR_MISMATCH by error3<PsiElement, IrSymbolWithModule, IrSymbolWithModule, ExpectActualMatchingCompatibility.Mismatch>(
        SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER
    )
    val EXPECT_ACTUAL_IR_INCOMPATIBILITY by error3<PsiElement, IrSymbolWithModule, IrSymbolWithModule, ExpectActualIncompatibility<*>>(
        SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER
    )
    val ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT by warning3<PsiElement, IrSymbol, IrSymbolWithModule, ExpectActualAnnotationsIncompatibilityType<IrAnnotation>>(
        SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER
    )
    val ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE by error1<PsiElement, IrValueParameter>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT by error1<PsiElement, IrSymbol>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)
    val KOTLIN_ACTUAL_ANNOTATION_MISSING by error1<PsiElement, IrSymbol>(SourceElementPositioningStrategies.EXPECT_ACTUAL_MODIFIER)

    val JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_EXPECT_FUNCTION by error1<PsiElement, IrSymbol>()
    val JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_ACTUAL_FUNCTION by error1<PsiElement, IrSymbol>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultIrActualizationErrorMessages
    }
}

internal object KtDefaultIrActualizationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            IrActualizationErrors.AMBIGUOUS_ACTUALS,
            "The {0} has several compatible ''actual'' declarations in module(s): {1}.",
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            Renderer { it.joinToString { IrActualizationDiagnosticRenderers.MODULE_WITH_PLATFORM.render(it) } },
        )
        map.put(
            IrActualizationErrors.NO_ACTUAL_FOR_EXPECT,
            "The {0} has no ''actual'' declaration in module ''{1}''.",
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.MODULE_WITH_PLATFORM,
        )
        map.put(
            IrActualizationErrors.EXPECT_ACTUAL_IR_MISMATCH,
            "The {0} doesn''t match the {1} because {2}.",
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.MISMATCH
        )
        map.put(
            IrActualizationErrors.EXPECT_ACTUAL_IR_INCOMPATIBILITY,
            "The following declarations are incompatible.\n  {0}\n  {1}\n  reason: {2}",
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.INCOMPATIBILITY
        )
        map.put(
            IrActualizationErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
            "{2}.\n" +
                    "All annotations from the ''expect'' declaration ''{0}'' must be present and have the same arguments on the {1}, otherwise they might behave incorrectly.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
            IrActualizationDiagnosticRenderers.ACTUALIZATION_SYMBOL_WITH_MODULE,
            IrActualizationDiagnosticRenderers.EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY,
        )
        map.put(
            IrActualizationErrors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE,
            "Parameter ''{0}'' has conflicting values in ''expect'' and ''actual'' annotations.",
            IrDiagnosticRenderers.DECLARATION_NAME,
        )
        map.put(
            IrActualizationErrors.JAVA_DIRECT_ACTUAL_WITHOUT_EXPECT,
            "The Java direct actual declaration ''{0}'' has no corresponding ''expect'' declaration.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
        )
        map.put(
            IrActualizationErrors.KOTLIN_ACTUAL_ANNOTATION_MISSING,
            "The corresponding Java declaration ''{0}'' must be marked with ''@kotlin.jvm.KotlinActual''.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
        )
        map.put(
            IrActualizationErrors.JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_EXPECT_FUNCTION,
            "Default parameters in the ''expect'' function ''{0}'' are not allowed because the function is actualized via the ''@KotlinActual'' annotation.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
        )
        map.put(
            IrActualizationErrors.JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_ACTUAL_FUNCTION,
            "Default parameters in the actual function ''{0}'' are not allowed because actualization is performed via the ''@KotlinActual'' annotation.",
            IrDiagnosticRenderers.SYMBOL_OWNER_DECLARATION_FQ_NAME,
        )
    }
}

internal object IrActualizationDiagnosticRenderers {
    val MISMATCH = Renderer<ExpectActualMatchingCompatibility.Mismatch> {
        it.reason
    }
    val INCOMPATIBILITY = Renderer<ExpectActualIncompatibility<*>> {
        it.reason
    }
    val EXPECT_ACTUAL_ANNOTATION_INCOMPATIBILITY =
        Renderer { incompatibilityType: ExpectActualAnnotationsIncompatibilityType<IrAnnotation> ->
            val expectAnnotation = ANNOTATION.render(incompatibilityType.expectAnnotation)
            val reason = when (incompatibilityType) {
                is ExpectActualAnnotationsIncompatibilityType.MissingOnActual -> "is missing on actual declaration"
                is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual -> {
                    val actualAnnotation = ANNOTATION.render(incompatibilityType.actualAnnotation)
                    "has different arguments on actual declaration: '$actualAnnotation'"
                }
            }
            "Annotation `$expectAnnotation` $reason"
        }

    private val ANNOTATION = Renderer<IrAnnotation> {
        "@" + RenderIrElementVisitor().renderAsAnnotation(it)
    }

    @JvmField
    val MODULE_WITH_PLATFORM = Renderer<ModuleInfoForDiagnostic> { module ->
        val platform = module.platform
        val platformNameIfAny = if (platform == null || platform.isCommon()) "" else " for " + platform.single().platformName
        module.name.asString() + platformNameIfAny
    }

    val ACTUALIZATION_SYMBOL_WITH_MODULE = Renderer<IrSymbolWithModule> { (symbol, module) ->
        val declaration = symbol.owner as? IrDeclarationWithName ?: return@Renderer "unknown name"
        buildString {
            when {
                module.platform == null -> append("binary")
                declaration.isExpect -> append("'expect'")
                else -> append("'actual'")
            }
            append(" declaration '")
            append(declaration.fqNameWithoutFileClassesWhenAvailable)
            append("' from '")
            append(MODULE_WITH_PLATFORM.render(module))
            append("'")
        }
    }
}

data class ModuleInfoForDiagnostic(val name: Name, val platform: TargetPlatform?)
data class IrSymbolWithModule(val symbol: IrSymbol, val module: ModuleInfoForDiagnostic)

internal fun IrSymbol.withModule() = IrSymbolWithModule(this, moduleInfoForDiagnostic())

internal fun IrSymbol.moduleInfoForDiagnostic(): ModuleInfoForDiagnostic {
    return (owner as IrDeclaration).moduleFragment.toModuleInfoForDiagnostic()
}

internal fun IrModuleFragment.toModuleInfoForDiagnostic(): ModuleInfoForDiagnostic {
    return ModuleInfoForDiagnostic(
        name = descriptor.getCapability(ModuleInfo.Capability)?.name ?: name,
        platform = descriptor.platform,
    )
}
