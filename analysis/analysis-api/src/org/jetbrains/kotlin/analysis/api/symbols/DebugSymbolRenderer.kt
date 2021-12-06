/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.KtNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.components.KtSymbolInfoProviderMixIn
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossiblyNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.extensionReceiverParameter

public object DebugSymbolRenderer {
    public fun render(symbol: KtSymbol): String = prettyPrint { renderSymbol(symbol) }

    public fun renderAnnotationApplication(application: KtAnnotationApplication): String =
        prettyPrint { renderAnnotationApplication(application) }

    public fun renderType(type: KtType): String = prettyPrint { renderType(type) }

    public fun KtAnalysisSession.renderExtra(symbol: KtSymbol): String = prettyPrint {
        renderSymbol(symbol)

        withIndent {
            @Suppress("DEPRECATION")
            (symbol as? KtCallableSymbol)?.getDispatchReceiverType()?.let { dispatchType ->
                appendLine().append("getDispatchReceiver()").append(": ")
                renderType(dispatchType)
            }

            KtSymbolInfoProviderMixIn::class.declaredMemberExtensionProperties
                .asSequence()
                .filter { (it.extensionReceiverParameter?.type?.classifier as? KClass<*>)?.isInstance(symbol) == true }
                .forEach { renderProperty(it, this@renderExtra, symbol) }
        }
    }

    public fun KtAnalysisSession.renderForSubstitutionOverrideUnwrappingTest(symbol: KtSymbol): String = prettyPrint {
        if (symbol !is KtCallableSymbol) return@prettyPrint

        renderSymbolHeader(symbol)

        withIndent {
            renderProperty(KtCallableSymbol::callableIdIfNonLocal, symbol)
            if (symbol is KtNamedSymbol) {
                renderProperty(KtNamedSymbol::name, symbol)
            }
            renderProperty(KtCallableSymbol::origin, symbol)
        }
    }

    private fun PrettyPrinter.renderProperty(property: KProperty<*>, vararg args: Any) {
        try {
            appendLine().append(property.name).append(": ")
            renderValue(property.getter.call(*args))
        } catch (e: InvocationTargetException) {
            append("Could not render due to ").appendLine(e.cause.toString())
        }
    }

    private fun PrettyPrinter.renderSymbol(symbol: KtSymbol) {
        renderSymbolHeader(symbol)
        val apiClass = getSymbolApiClass(symbol)
        withIndent {
            apiClass.members
                .asSequence()
                .filterIsInstance<KProperty<*>>()
                .filter { it.name !in ignoredPropertyNames }
                .sortedBy { it.name }
                .forEach { renderProperty(it, symbol) }
        }
    }

    private fun PrettyPrinter.renderSymbolHeader(symbol: KtSymbol) {
        val apiClass = getSymbolApiClass(symbol)
        append(apiClass.simpleName).append(':')
    }

    private fun PrettyPrinter.renderList(values: List<*>) {
        if (values.isEmpty()) {
            append("[]")
            return
        }

        withIndentInSquareBrackets {
            printCollection(values, separator = "\n") { renderValue(it) }
        }
    }

    private fun PrettyPrinter.renderSymbolTag(symbol: KtSymbol) {
        fun renderId(id: Any?, symbol: KtSymbol) {
            if (id != null) {
                renderValue(id)
            } else {
                val outerName = (symbol as? KtPossiblyNamedSymbol)?.name ?: SpecialNames.NO_NAME_PROVIDED
                append("<local>/" + outerName.asString())
            }
        }

        append(getSymbolApiClass(symbol).simpleName)
        append("(")
        when (symbol) {
            is KtClassLikeSymbol -> renderId(symbol.classIdIfNonLocal, symbol)
            is KtValueParameterSymbol -> renderValue(symbol.name)
            is KtPropertyGetterSymbol -> append("<getter>")
            is KtPropertySetterSymbol -> append("<setter>")
            is KtCallableSymbol -> renderId(symbol.callableIdIfNonLocal, symbol)
            is KtNamedSymbol -> renderValue(symbol.name)
            else -> error("Unsupported symbol ${symbol::class.java.name}")
        }
        append(")")
    }

    private fun PrettyPrinter.renderAnnotationValue(value: KtAnnotationValue) {
        append(KtAnnotationValueRenderer.render(value))
    }

    private fun PrettyPrinter.renderNamedConstantValue(value: KtNamedAnnotationValue) {
        append(value.name.render()).append(" = ")
        renderValue(value.expression)
    }

    private fun PrettyPrinter.renderType(type: KtType) {
        if (type.annotations.isNotEmpty()) {
            renderList(type.annotations)
            append(' ')
        }
        when (type) {
            is KtClassErrorType -> append("ERROR_TYPE")
            else -> append(type.asStringForDebugging())
        }
    }

    private fun PrettyPrinter.renderAnnotationApplication(call: KtAnnotationApplication) {
        renderValue(call.classId)
        append('(')
        call.arguments.sortedBy { it.name }.forEachIndexed { index, value ->
            if (index > 0) {
                append(", ")
            }
            renderValue(value)
        }
        append(')')

        withIndent {
            appendLine().append("psi: ")
            renderValue(call.psi?.javaClass?.simpleName)
        }
    }

    private fun PrettyPrinter.renderDeprecationInfo(info: DeprecationInfo) {
        append("DeprecationInfo(")
        append("deprecationLevel=${info.deprecationLevel}, ")
        append("propagatesToOverrides=${info.propagatesToOverrides}, ")
        append("message=${info.message}")
        append(")")
    }

    private fun PrettyPrinter.renderValue(value: Any?) {
        when (value) {
            // Symbol-related values
            is KtSymbol -> renderSymbolTag(value)
            is KtType -> renderType(value)
            is KtAnnotationValue -> renderAnnotationValue(value)
            is KtNamedAnnotationValue -> renderNamedConstantValue(value)
            is KtInitializerValue -> renderKtInitializerValue(value)
            is KtAnnotationApplication -> renderAnnotationApplication(value)
            is KtAnnotationsList -> renderAnnotationsList(value)
            // Other custom values
            is Name -> append(value.asString())
            is FqName -> append(value.asString())
            is ClassId -> append(value.asString())
            is DeprecationInfo -> renderDeprecationInfo(value)
            is Visibility -> append(value::class.java.simpleName)
            // Unsigned integers
            is UByte -> append(value.toString())
            is UShort -> append(value.toString())
            is UInt -> append(value.toString())
            is ULong -> append(value.toString())
            // Java values
            is Enum<*> -> append(value.name)
            is List<*> -> renderList(value)
            else -> append(value.toString())
        }
    }

    private fun PrettyPrinter.renderKtInitializerValue(value: KtInitializerValue) {
        when (value) {
            is KtConstantInitializerValue -> {
                append("KtConstantInitializerValue(")
                append(value.constant.renderAsKotlinConstant())
                append(")")
            }
            is KtNonConstantInitializerValue -> {
                append("KtNonConstantInitializerValue(")
                append(value.initializerPsi?.firstLineOfPsi() ?: "NO_PSI")
                append(")")
            }
        }
    }

    private fun PrettyPrinter.renderAnnotationsList(value: KtAnnotationsList) {
        renderList(value.annotations)
    }

    private fun getSymbolApiClass(symbol: KtSymbol): KClass<*> {
        var current: Class<in KtSymbol> = symbol.javaClass

        while (true) {
            val className = current.name
            if (symbolImplementationPackageNames.none { className.startsWith("$it.") }) {
                return current.kotlin
            }
            current = current.superclass
        }
    }

    private fun PsiElement.firstLineOfPsi(): String {
        val text = text
        val lines = text.lines()
        return if (lines.count() <= 1) text
        else lines.first() + " ..."
    }

    private val ignoredPropertyNames = setOf("psi", "token")

    private val symbolImplementationPackageNames = listOf(
        "org.jetbrains.kotlin.analysis.api.fir",
        "org.jetbrains.kotlin.analysis.api.descriptors",
    )
}