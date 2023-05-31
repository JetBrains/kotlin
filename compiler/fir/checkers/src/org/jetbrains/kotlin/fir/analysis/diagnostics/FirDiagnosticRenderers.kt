/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderReadableWithFqNames
import org.jetbrains.kotlin.name.CallableId

object FirDiagnosticRenderers {
    @OptIn(SymbolInternals::class)
    val SYMBOL = Renderer { symbol: FirBasedSymbol<*> ->
        when (symbol) {
            is FirClassLikeSymbol<*>,
            is FirCallableSymbol<*> -> FirRenderer(
                typeRenderer = ConeTypeRenderer(),
                idRenderer = ConeIdShortRenderer(),
                classMemberRenderer = FirNoClassMemberRenderer(),
                bodyRenderer = null,
                propertyAccessorRenderer = null,
                callArgumentsRenderer = FirCallNoArgumentsRenderer(),
                modifierRenderer = FirPartialModifierRenderer(),
                valueParameterRenderer = FirValueParameterRendererNoDefaultValue(),
            ).renderElementAsString(symbol.fir, trim = true)
            is FirTypeParameterSymbol -> symbol.name.asString()
            else -> "???"
        }
    }

    val SYMBOLS = KtDiagnosticRenderers.COLLECTION(SYMBOL)

    val SYMBOLS_ON_NEWLINE_WITH_INDENT = object : ContextIndependentParameterRenderer<Collection<FirCallableSymbol<*>>> {
        private val mode = MultiplatformDiagnosticRenderingMode()

        override fun render(obj: Collection<FirCallableSymbol<*>>): String {
            return buildString {
                for (symbol in obj) {
                    mode.newLine(this)
                    mode.renderSymbol(this, symbol, "")
                }
            }
        }
    }

    val RENDER_COLLECTION_OF_TYPES = Renderer { types: Collection<ConeKotlinType> ->
        types.joinToString(separator = ", ") { type ->
            RENDER_TYPE.render(type)
        }
    }

    val VARIABLE_NAME = Renderer { symbol: FirVariableSymbol<*> ->
        symbol.name.asString()
    }

    val FIR = Renderer { element: FirElement ->
        element.render()
    }

    val DECLARATION_NAME = Renderer { symbol: FirBasedSymbol<*> ->
        val name = when (symbol) {
            is FirCallableSymbol<*> -> symbol.name
            is FirClassLikeSymbol<*> -> symbol.classId.shortClassName
            else -> return@Renderer "???"
        }
        name.asString()
    }

    val RENDER_CLASS_OR_OBJECT = Renderer { classSymbol: FirClassSymbol<*> ->
        val name = classSymbol.classId.relativeClassName.asString()
        val classOrObject = when (classSymbol.classKind) {
            ClassKind.OBJECT -> "Object"
            ClassKind.INTERFACE -> "Interface"
            else -> "Class"
        }
        "$classOrObject $name"
    }

    val RENDER_CLASS_OR_OBJECT_NAME = Renderer { firClassLike: FirClassLikeSymbol<*> ->
        val name = firClassLike.classId.relativeClassName.shortName().asString()
        val prefix = when (firClassLike) {
            is FirTypeAliasSymbol -> "typealias"
            is FirRegularClassSymbol -> {
                when {
                    firClassLike.isCompanion -> "companion object"
                    firClassLike.isInterface -> "interface"
                    firClassLike.isEnumClass -> "enum class"
                    firClassLike.isFromEnumClass -> "enum entry"
                    firClassLike.isLocalClassOrAnonymousObject -> "object"
                    else -> "class"
                }
            }
            else -> AssertionError("Unexpected class: $firClassLike")
        }
        "$prefix '$name'"
    }

    val RENDER_TYPE = Renderer { t: ConeKotlinType ->
        // TODO: need a way to tune granuality, e.g., without parameter names in functional types.
        t.renderReadableWithFqNames()
    }

    // TODO: properly implement
    val RENDER_TYPE_WITH_ANNOTATIONS = RENDER_TYPE

    val FQ_NAMES_IN_TYPES = Renderer { symbol: FirBasedSymbol<*> ->
        @OptIn(SymbolInternals::class)
        FirRenderer(
            annotationRenderer = null, bodyRenderer = null, idRenderer = ConeIdFullRenderer()
        ).renderElementAsString(symbol.fir, trim = true)
    }

    val AMBIGUOUS_CALLS = Renderer { candidates: Collection<FirBasedSymbol<*>> ->
        candidates.joinToString(separator = "\n", prefix = "\n") { symbol ->
            SYMBOL.render(symbol)
        }
    }

    private const val WHEN_MISSING_LIMIT = 7

    val WHEN_MISSING_CASES = Renderer { missingCases: List<WhenMissingCase> ->
        if (missingCases.firstOrNull() == WhenMissingCase.Unknown) {
            "'else' branch"
        } else {
            val list = missingCases.joinToString(", ", limit = WHEN_MISSING_LIMIT) { "'$it'" }
            val branches = if (missingCases.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        }
    }

    val MODULE_DATA = Renderer<FirModuleData> {
        "Module ${it.name}"
    }

    val NAME_OF_CONTAINING_DECLARATION_OR_FILE = Renderer { symbol: CallableId ->
        val classId = symbol.classId
        if (classId == null) {
            "file"
        } else {
            "'${classId}'"
        }
    }

    val FUNCTIONAL_TYPE_KIND = Renderer { kind: FunctionTypeKind ->
        kind.prefixForTypeRender ?: kind.classNamePrefix
    }

    val FUNCTIONAL_TYPE_KINDS = KtDiagnosticRenderers.COLLECTION(FUNCTIONAL_TYPE_KIND)

    @Suppress("FunctionName")
    fun <T> COLLECTION(renderer: ContextIndependentParameterRenderer<T>): ContextIndependentParameterRenderer<Collection<T>> {
        return Renderer { list ->
            list.joinToString(prefix = "[", postfix = "]", separator = ", ", limit = 3, truncated = "...") {
                renderer.render(it)
            }
        }
    }
}
