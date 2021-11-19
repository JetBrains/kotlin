/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.KtNonConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.components.KtSymbolInfoProviderMixIn
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.types.KtClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.*
import java.lang.Appendable
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.extensionReceiverParameter

public object DebugSymbolRenderer {
    public fun render(symbol: KtSymbol): String = Block().apply { renderSymbol(symbol) }.toString()

    public fun renderAnnotationApplication(application: KtAnnotationApplication): String =
        Block().apply { renderAnnotationApplication(application) }.toString()

    public fun renderType(type: KtType): String = Block().apply { renderType(type) }.toString()

    public fun KtAnalysisSession.renderExtra(symbol: KtSymbol): String = Block().apply {
        renderSymbol(symbol)

        withIndent {
            @Suppress("DEPRECATION")
            (symbol as? KtPossibleMemberSymbol)?.getDispatchReceiverType()?.let { dispatchType ->
                appendLine().append("getDispatchReceiver()").append(": ")
                renderType(dispatchType)
            }

            KtSymbolInfoProviderMixIn::class.declaredMemberExtensionProperties
                .asSequence()
                .filter { (it.extensionReceiverParameter?.type?.classifier as? KClass<*>)?.isInstance(symbol) == true }
                .forEach { renderProperty(it, this@renderExtra, symbol) }
        }
    }.toString()

    private fun Block.renderProperty(property: KProperty<*>, vararg args: Any) {
        try {
            appendLine().append(property.name).append(": ")
            renderValue(property.getter.call(*args))
        } catch (e: InvocationTargetException) {
            append("Could not render due to ").appendLine(e.cause.toString())
        }
    }

    private fun Block.renderSymbol(symbol: KtSymbol) {
        val apiClass = getSymbolApiClass(symbol)
        append(apiClass.simpleName).append(':')

        withIndent {
            apiClass.members
                .asSequence()
                .filterIsInstance<KProperty<*>>()
                .filter { it.name !in ignoredPropertyNames }
                .sortedBy { it.name }
                .forEach { renderProperty(it, symbol) }
        }
    }

    private fun Block.renderList(values: List<*>) {
        if (values.isEmpty()) {
            append("[]")
            return
        }

        append('[')
        withIndent {
            for (value in values) {
                appendLine()
                renderValue(value)
            }
        }
        appendLine().append(']')
    }

    private fun Block.renderSymbolTag(symbol: KtSymbol) {
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

    private fun Block.renderConstantValue(value: KtConstantValue) {
        when (value) {
            is KtLiteralConstantValue<*> -> renderValue(value.value)
            is KtEnumEntryConstantValue -> {
                append(KtEnumEntryConstantValue::class.java.simpleName)
                append("(")
                renderValue(value.callableId)
                append(")")
            }
            else -> append(value::class.java.simpleName)
        }
    }

    private fun Block.renderNamedConstantValue(value: KtNamedConstantValue) {
        append(value.name).append(" = ")
        renderValue(value.expression)
    }

    private fun Block.renderType(type: KtType) {
        if (type.annotations.isNotEmpty()) {
            renderList(type.annotations)
            append(' ')
        }
        when (type) {
            is KtClassErrorType -> append("ERROR_TYPE")
            else -> append(type.asStringForDebugging())
        }
    }

    private fun Block.renderAnnotationApplication(call: KtAnnotationApplication) {
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

    private fun Block.renderDeprecationInfo(info: DeprecationInfo) {
        append("DeprecationInfo(")
        append("deprecationLevel=${info.deprecationLevel}, ")
        append("propagatesToOverrides=${info.propagatesToOverrides}, ")
        append("message=${info.message}")
        append(")")
    }

    private fun Block.renderValue(value: Any?) {
        when (value) {
            // Symbol-related values
            is KtSymbol -> renderSymbolTag(value)
            is KtType -> renderType(value)
            is KtConstantValue -> renderConstantValue(value)
            is KtNamedConstantValue -> renderNamedConstantValue(value)
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

    private fun Block.renderKtInitializerValue(value: KtInitializerValue) {
        when (value) {
            is KtConstantInitializerValue -> {
                append("KtConstantInitializerValue(")
                renderConstantValue(value.constant)
                append(")")
            }
            is KtNonConstantInitializerValue -> {
                append("KtNonConstantInitializerValue(")
                append(value.initializerPsi?.firstLineOfPsi() ?: "NO_PSI")
                append(")")
            }
        }
    }

    private fun Block.renderAnnotationsList(value: KtAnnotationsList) {
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

private class Block : Appendable {
    private val builder = StringBuilder()
    private var indent = 0

    override fun append(seq: CharSequence): Appendable = apply {
        seq.split('\n').forEachIndexed { index, line ->
            if (index > 0) {
                builder.append('\n')
            }
            appendIndentIfNeeded()
            builder.append(line)
        }
    }

    override fun append(seq: CharSequence, start: Int, end: Int): Appendable = apply {
        append(seq.subSequence(start, end))
    }

    override fun append(c: Char): Appendable = apply {
        if (c != '\n') {
            appendIndentIfNeeded()
        }
        builder.append(c)
    }

    private fun appendIndentIfNeeded() {
        if (builder.isEmpty() || builder[builder.lastIndex] == '\n') {
            builder.append(" ".repeat(2 * indent))
        }
    }

    fun withIndent(block: Block.() -> Unit) {
        indent += 1
        block(this)
        indent -= 1
    }

    override fun toString(): String {
        return builder.toString()
    }
}