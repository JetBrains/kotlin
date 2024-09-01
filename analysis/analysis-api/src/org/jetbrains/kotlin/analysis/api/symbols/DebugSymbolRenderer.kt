/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.Context
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.renderKaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.utils.getApiKClassOf
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.types.Variance
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

@KaNonPublicApi
@OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
public class DebugSymbolRenderer(
    public val renderExtra: Boolean = false,
    public val renderTypeByProperties: Boolean = false,
    public val renderExpandedTypes: Boolean = false,
) {

    public fun render(analysisSession: KaSession, symbol: KaSymbol): String {
        return prettyPrint { analysisSession.renderSymbol(symbol, this@prettyPrint) }
    }

    public fun renderAnnotationApplication(analysisSession: KaSession, application: KaAnnotation): String {
        return prettyPrint { analysisSession.renderAnnotationApplication(application, this@prettyPrint) }
    }

    public fun renderType(analysisSession: KaSession, type: KaType): String {
        return prettyPrint { analysisSession.renderType(type, this@prettyPrint) }
    }

    private fun KaSession.renderSymbol(symbol: KaSymbol, printer: PrettyPrinter) {
        renderSymbolInternals(symbol, printer)

        if (!renderExtra) return
        printer.withIndent {
            @Suppress("DEPRECATION")
            (symbol as? KaCallableSymbol)?.dispatchReceiverType?.let { dispatchType ->
                appendLine().append("getDispatchReceiver()").append(": ")
                renderType(dispatchType, printer)
            }

            renderComputedValue("getContainingFileSymbol", printer) { symbol.containingFile }

            if (symbol is KaCallableSymbol) {
                renderComputedValue("getContainingJvmClassName", printer) { symbol.containingJvmClassName }
            }

            renderComputedValue("getContainingModule", printer) { symbol.containingModule }

            if (symbol is KaClassSymbol) {
                renderComputedValue("annotationApplicableTargets", printer) { symbol.annotationApplicableTargets }
            }

            renderComputedValue("deprecationStatus", printer) { symbol.deprecationStatus }

            if (symbol is KaPropertySymbol) {
                renderComputedValue("getterDeprecationStatus", printer) { symbol.getterDeprecationStatus }
                renderComputedValue("javaGetterName", printer) { symbol.javaGetterName }
                renderComputedValue("javaSetterName", printer) { symbol.javaSetterName }
                renderComputedValue("setterDeprecationStatus", printer) { symbol.setterDeprecationStatus }
            }
        }
    }

    public fun KaSession.renderForSubstitutionOverrideUnwrappingTest(symbol: KaSymbol): String = prettyPrint {
        if (symbol !is KaCallableSymbol) return@prettyPrint

        renderFrontendIndependentKClassNameOf(symbol, printer)

        withIndent {
            appendLine()
            renderProperty(KaCallableSymbol::callableId, printer, renderSymbolsFully = false, symbol)
            if (symbol is KaNamedSymbol) {
                appendLine()
                renderProperty(KaNamedSymbol::name, printer, renderSymbolsFully = false, symbol)
            }
            appendLine()
            renderProperty(KaCallableSymbol::origin, printer, renderSymbolsFully = false, symbol)
        }
    }

    private fun KaSession.renderComputedValue(name: String, printer: PrettyPrinter, block: () -> Any?) {
        printer.appendLine()
        printer.append(name).append(": ")

        val value = try {
            block()
        } catch (e: Throwable) {
            printer.append("Could not render due to ").appendLine(e.toString())
            return
        }

        renderValue(value, printer, renderSymbolsFully = false)
    }

    private fun KaSession.renderProperty(
        property: KProperty<*>,
        printer: PrettyPrinter,
        renderSymbolsFully: Boolean,
        vararg args: Any,
    ) {
        printer.append(property.name).append(": ")
        renderFunctionCall(property.getter, printer, renderSymbolsFully, args)
    }

    private fun KaSession.renderFunctionCall(
        function: KFunction<*>,
        printer: PrettyPrinter,
        renderSymbolsFully: Boolean,
        args: Array<out Any>,
    ) {
        try {
            function.isAccessible = true
            renderValue(function.call(*args), printer, renderSymbolsFully)
        } catch (e: InvocationTargetException) {
            printer.append("Could not render due to ").appendLine(e.cause.toString())
        }
    }

    private fun KaSession.renderSymbolInternals(symbol: KaSymbol, printer: PrettyPrinter) {
        renderFrontendIndependentKClassNameOf(symbol, printer)
        val apiClass = getApiKClassOf(symbol)
        printer.withIndent {
            val members = apiClass.members
                .filterIsInstance<KProperty<*>>()
                .filter { !it.hasAnnotation<Deprecated>() && it.name !in ignoredPropertyNames }
                .sortedBy { it.name }
            appendLine()
            printCollectionIfNotEmpty(members, separator = "\n") { member ->
                val renderSymbolsFully = member.name == KaValueParameterSymbol::generatedPrimaryConstructorProperty.name
                renderProperty(member, printer, renderSymbolsFully, symbol)
            }
        }
    }

    private fun renderFrontendIndependentKClassNameOf(instanceOfClassToRender: Any, printer: PrettyPrinter) {
        val apiClass = getApiKClassOf(instanceOfClassToRender)
        printer.append(apiClass.simpleName).append(':')
    }

    private fun KaSession.renderList(values: List<*>, printer: PrettyPrinter, renderSymbolsFully: Boolean) {
        if (values.isEmpty()) {
            printer.append("[]")
            return
        }

        printer.withIndentInSquareBrackets {
            printCollection(values, separator = "\n") { renderValue(it, printer, renderSymbolsFully) }
        }
    }

    private fun KaSession.renderSymbolTag(symbol: KaSymbol, printer: PrettyPrinter, renderSymbolsFully: Boolean) {
        fun renderId(id: Any?, symbol: KaSymbol) {
            if (id != null) {
                renderValue(id, printer, renderSymbolsFully)
            } else {
                val outerName = symbol.name ?: SpecialNames.NO_NAME_PROVIDED
                printer.append("<local>/" + outerName.asString())
            }
        }

        if (renderSymbolsFully || symbol is KaBackingFieldSymbol || symbol is KaPropertyAccessorSymbol || symbol is KaParameterSymbol) {
            renderSymbol(symbol, printer)
            return
        }

        with(printer) {
            append(getApiKClassOf(symbol).simpleName)
            append("(")
            when (symbol) {
                is KaClassLikeSymbol -> renderId(symbol.classId, symbol)
                is KaCallableSymbol -> renderId(symbol.callableId, symbol)
                is KaNamedSymbol -> renderValue(symbol.name, printer, renderSymbolsFully = false)
                is KaFileSymbol -> renderValue((symbol.psi as KtFile).name, printer, renderSymbolsFully = false)
                else -> error("Unsupported symbol ${symbol::class}")
            }
            append(")")
        }
    }

    private fun renderAnnotationValue(value: KaAnnotationValue, printer: PrettyPrinter) {
        printer.append(KaAnnotationValueRenderer.render(value))
    }

    private fun KaSession.renderNamedConstantValue(value: KaNamedAnnotationValue, printer: PrettyPrinter) {
        printer.append(value.name.render()).append(" = ")
        renderValue(value.expression, printer, renderSymbolsFully = false)
    }

    private fun KaSession.renderType(type: KaType, printer: PrettyPrinter) {
        val typeToRender = if (renderExpandedTypes) type.fullyExpandedType else type

        renderFrontendIndependentKClassNameOf(typeToRender, printer)
        printer.withIndent {
            appendLine()
            if (renderTypeByProperties) {
                renderByPropertyNames(typeToRender, printer)
            } else {
                append("annotations: ")
                renderAnnotationsList(typeToRender.annotations, printer)

                if (typeToRender is KaClassType) {
                    appendLine()
                    append("typeArguments: ")
                    renderList(typeToRender.typeArguments, printer, renderSymbolsFully = false)
                }

                appendLine()
                append("type: ")
                when (typeToRender) {
                    is KaErrorType -> append("ERROR_TYPE")
                    else -> append(typeToRender.toString())
                }
            }
        }
    }

    private fun KaSession.renderByPropertyNames(value: Any, printer: PrettyPrinter) {
        val members = value::class.members
            .filter { it.name !in ignoredPropertyNames }
            .filter { it.visibility != KVisibility.PRIVATE && it.visibility != KVisibility.INTERNAL }
            .filter { !it.hasAnnotation<Deprecated>() }
            .sortedBy { it.name }
            .filterIsInstance<KProperty<*>>()

        printer.printCollectionIfNotEmpty(members, separator = "\n") { member ->
            renderProperty(member, printer, renderSymbolsFully = false, value)
        }
    }

    private fun KaSession.renderAnnotationApplication(call: KaAnnotation, printer: PrettyPrinter) {
        with(printer) {
            renderValue(call.classId, printer, renderSymbolsFully = false)
            append('(')
            call.arguments.sortedBy { it.name }.forEachIndexed { index, value ->
                if (index > 0) {
                    append(", ")
                }
                renderValue(value, printer, renderSymbolsFully = false)
            }
            append(')')

            withIndent {
                appendLine().append("psi: ")
                val psi =
                    if (call.psi?.containingKtFile?.isCompiled == true) {
                        null
                    } else call.psi
                renderValue(psi?.javaClass?.simpleName, printer, renderSymbolsFully = false)
            }
        }
    }

    private fun renderDeprecationInfo(info: DeprecationInfo, printer: PrettyPrinter) {
        with(printer) {
            append("DeprecationInfo(")
            append("deprecationLevel=${info.deprecationLevel}, ")
            append("propagatesToOverrides=${info.propagatesToOverrides}, ")
            append("message=${info.message}")
            append(")")
        }
    }

    private fun KaSession.renderValue(value: Any?, printer: PrettyPrinter, renderSymbolsFully: Boolean) {
        when (value) {
            // Symbol-related values
            is KaSymbol -> renderSymbolTag(value, printer, renderSymbolsFully)
            is KaType -> renderType(value, printer)
            is KaTypeProjection -> renderTypeProjection(value, printer)
            is KaClassTypeQualifier -> renderTypeQualifier(value, printer)
            is KaAnnotationValue -> renderAnnotationValue(value, printer)
            is KaContractEffectDeclaration -> Context(this@KaSession, printer, this@DebugSymbolRenderer)
                .renderKaContractEffectDeclaration(value, endWithNewLine = false)
            is KaNamedAnnotationValue -> renderNamedConstantValue(value, printer)
            is KaInitializerValue -> renderKtInitializerValue(value, printer)
            is KaContextReceiver -> renderContextReceiver(value, printer)
            is KaAnnotation -> renderAnnotationApplication(value, printer)
            is KaAnnotationList -> renderAnnotationsList(value, printer)
            is KaModule -> renderModule(value, printer)
            // Other custom values
            is Name -> printer.append(value.asString())
            is FqName -> printer.append(value.asString())
            is ClassId -> printer.append(value.asString())
            is DeprecationInfo -> renderDeprecationInfo(value, printer)
            is Visibility -> printer.append(value::class.java.simpleName)
            // Unsigned integers
            is UByte -> printer.append(value.toString())
            is UShort -> printer.append(value.toString())
            is UInt -> printer.append(value.toString())
            is ULong -> printer.append(value.toString())
            // Java values
            is Enum<*> -> printer.append(value.name)
            is List<*> -> renderList(value, printer, renderSymbolsFully = false)
            else -> printer.append(value.toString())
        }
    }

    private fun KaSession.renderTypeProjection(value: KaTypeProjection, printer: PrettyPrinter) {
        when (value) {
            is KaStarTypeProjection -> printer.append("*")
            is KaTypeArgumentWithVariance -> {
                if (value.variance != Variance.INVARIANT) {
                    printer.append("${value.variance.label} ")
                }
                renderType(value.type, printer)
            }
        }
    }

    private fun KaSession.renderTypeQualifier(value: KaClassTypeQualifier, printer: PrettyPrinter) {
        with(printer) {
            appendLine("qualifier:")
            withIndent {
                renderByPropertyNames(value, printer)
            }
        }
    }

    private fun KaSession.renderContextReceiver(receiver: KaContextReceiver, printer: PrettyPrinter) {
        with(printer) {
            append("KtContextReceiver:")
            withIndent {
                appendLine()
                append("label: ")
                renderValue(receiver.label, printer, renderSymbolsFully = false)
                appendLine()
                append("type: ")
                renderType(receiver.type, printer)
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun renderModule(module: KaModule, printer: PrettyPrinter) {
        val apiClass = when (val moduleClass = module::class) {
            in kaModuleApiSubclasses -> moduleClass
            else -> moduleClass.allSuperclasses.first { it in kaModuleApiSubclasses }
        }
        printer.append(apiClass.simpleName + " \"" + module.moduleDescription + "\"")
    }

    private fun KClass<*>.allSealedSubClasses(): List<KClass<*>> = buildList {
        add(this@allSealedSubClasses)
        sealedSubclasses.flatMapTo(this) { it.allSealedSubClasses() }
    }

    /**
     * All [KaModule] classes which are part of the API (defined in `KaModule.kt`) and should be printed in test data.
     */
    @OptIn(KaPlatformInterface::class, KaExperimentalApi::class)
    private val kaModuleApiSubclasses =
        listOf(
            KaModule::class,
            KaSourceModule::class,
            KaLibraryModule::class,
            KaLibrarySourceModule::class,
            KaBuiltinsModule::class,
            KaScriptModule::class,
            KaScriptDependencyModule::class,
            KaDanglingFileModule::class,
            KaNotUnderContentRootModule::class,
        ).sortedWith { a, b ->
            when {
                a == b -> 0
                a.isSubclassOf(b) -> -1
                b.isSubclassOf(a) -> 1
                else -> 0
            }
        }

    private fun renderKtInitializerValue(value: KaInitializerValue, printer: PrettyPrinter) {
        with(printer) {
            when (value) {
                is KaConstantInitializerValue -> {
                    append("KtConstantInitializerValue(")
                    append(value.constant.render())
                    append(")")
                }

                is KaNonConstantInitializerValue -> {
                    append("KtNonConstantInitializerValue(")
                    append(value.initializerPsi?.firstLineOfPsi() ?: "NO_PSI")
                    append(")")
                }

                is KaConstantValueForAnnotation -> {
                    append("KtConstantValueForAnnotation(")
                    append(value.annotationValue.renderAsSourceCode())
                    append(")")
                }
            }
        }
    }

    private fun KaSession.renderAnnotationsList(value: KaAnnotationList, printer: PrettyPrinter) {
        renderList(value, printer, renderSymbolsFully = false)
    }

    private fun PsiElement.firstLineOfPsi(): String {
        val text = text
        val lines = text.lines()
        return if (lines.count() <= 1) text
        else lines.first() + " ..."
    }

    public companion object {
        private val ignoredPropertyNames = setOf(
            "psi",
            "token",
            "builder",
            "coneType",
            "analysisContext",
            "fe10Type",

            // These properties are made obsolete by their counterparts without `*IfNonLocal` (e.g. `classId`), which contain the same
            // values.
            "classIdIfNonLocal",
            "containingClassIdIfNonLocal",
            "callableIdIfNonLocal",
        )
    }
}

private val PrettyPrinter.printer: PrettyPrinter
    get() = this