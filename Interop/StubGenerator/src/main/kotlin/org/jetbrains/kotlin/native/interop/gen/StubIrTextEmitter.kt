/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.lang.IllegalStateException

/**
 * Emits stubs and bridge functions as *.kt and *.c files.
 * Many unintuitive printings are made for compatability with previous version of stub generator.
 *
 * [omitEmptyLines] is useful for testing output (e.g. diff calculating).
 */
class StubIrTextEmitter(
        private val context: StubIrContext,
        private val builderResult: StubIrBuilderResult,
        private val bridgeBuilderResult: BridgeBuilderResult,
        private val omitEmptyLines: Boolean = false
) {
    private val kotlinFile = bridgeBuilderResult.kotlinFile
    private val nativeBridges = bridgeBuilderResult.nativeBridges
    private val propertyAccessorBridgeBodies = bridgeBuilderResult.propertyAccessorBridgeBodies
    private val functionBridgeBodies = bridgeBuilderResult.functionBridgeBodies

    private val pkgName: String
        get() = context.configuration.pkgName

    private val jvmFileClassName = if (pkgName.isEmpty()) {
        context.libName
    } else {
        pkgName.substringAfterLast('.')
    }

    private val StubContainer.isTopLevelContainer: Boolean
        get() = this == builderResult.stubs

    companion object {
        private val VALID_PACKAGE_NAME_REGEX = "[a-zA-Z0-9_.]+".toRegex()
    }

    /**
     * The output currently used by the generator.
     * Should append line separator after any usage.
     */
    private var out: (String) -> Unit = {
        throw IllegalStateException()
    }

    private fun emitEmptyLine() {
        if (!omitEmptyLines) {
            out("")
        }
    }

    private fun <R> withOutput(output: (String) -> Unit, action: () -> R): R {
        val oldOut = out
        out = output
        try {
            return action()
        } finally {
            out = oldOut
        }
    }

    private fun <R> withOutput(appendable: Appendable, action: () -> R): R {
        return withOutput({ appendable.appendln(it) }, action)
    }

    private fun generateLinesBy(action: () -> Unit): List<String> {
        val result = mutableListOf<String>()
        withOutput({ result.add(it) }, action)
        return result
    }

    private fun generateKotlinFragmentBy(block: () -> Unit): Sequence<String> {
        val lines = generateLinesBy(block)
        return lines.asSequence()
    }

    private fun <R> indent(action: () -> R): R {
        val oldOut = out
        return withOutput({ oldOut("    $it") }, action)
    }

    private fun <R> block(header: String, body: () -> R): R {
        out("$header {")
        val res = indent {
            body()
        }
        out("}")
        return res
    }

    private fun emitKotlinFileHeader() {
        if (context.platform == KotlinPlatform.JVM) {
            out("@file:JvmName(${jvmFileClassName.quoteAsKotlinLiteral()})")
        }
        if (context.platform == KotlinPlatform.NATIVE) {
            out("@file:kotlinx.cinterop.InteropStubs")
        }

        val suppress = mutableListOf("UNUSED_VARIABLE", "UNUSED_EXPRESSION").apply {
            if (context.configuration.library.language == Language.OBJECTIVE_C) {
                add("CONFLICTING_OVERLOADS")
                add("RETURN_TYPE_MISMATCH_ON_INHERITANCE")
                add("PROPERTY_TYPE_MISMATCH_ON_INHERITANCE") // Multiple-inheriting property with conflicting types
                add("VAR_TYPE_MISMATCH_ON_INHERITANCE") // Multiple-inheriting mutable property with conflicting types
                add("RETURN_TYPE_MISMATCH_ON_OVERRIDE")
                add("WRONG_MODIFIER_CONTAINING_DECLARATION") // For `final val` in interface.
                add("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
                add("UNUSED_PARAMETER") // For constructors.
                add("MANY_IMPL_MEMBER_NOT_IMPLEMENTED") // Workaround for multiple-inherited properties.
                add("MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED") // Workaround for multiple-inherited properties.
                add("EXTENSION_SHADOWED_BY_MEMBER") // For Objective-C categories represented as extensions.
                add("REDUNDANT_NULLABLE") // This warning appears due to Obj-C typedef nullability incomplete support.
                add("DEPRECATION") // For uncheckedCast.
                add("DEPRECATION_ERROR") // For initializers.
            }
        }

        out("@file:Suppress(${suppress.joinToString { it.quoteAsKotlinLiteral() }})")
        if (pkgName != "") {
            val packageName = pkgName.split(".").joinToString("."){
                if(it.matches(VALID_PACKAGE_NAME_REGEX)){
                    it
                }else{
                    "`$it`"
                }
            }
            out("package $packageName")
            out("")
        }
        if (context.platform == KotlinPlatform.NATIVE) {
            out("import kotlin.native.SymbolName")
            out("import kotlinx.cinterop.internal.*")
        }
        out("import kotlinx.cinterop.*")

        kotlinFile.buildImports().forEach {
            out(it)
        }

        out("")

        out("// NOTE THIS FILE IS AUTO-GENERATED")
    }
    fun emit(ktFile: Appendable, cFile: Appendable, entryPoint: String?) {

        withOutput(cFile) {
            context.libraryForCStubs.preambleLines.forEach {
                out(it)
            }
            out("")

            out("// NOTE THIS FILE IS AUTO-GENERATED")
            out("")

            nativeBridges.nativeLines.forEach(out)

            if (entryPoint != null) {
                out("extern int Konan_main(int argc, char** argv);")
                out("")
                out("__attribute__((__used__))")
                out("int $entryPoint(int argc, char** argv)  {")
                out("  return Konan_main(argc, argv);")
                out("}")
            }
        }

        // Stubs generation may affect imports list so do it before header generation.
        val stubLines = generateKotlinFragmentBy {
            printer.visitSimpleStubContainer(builderResult.stubs, null)
        }

        withOutput(ktFile) {
            emitKotlinFileHeader()
            stubLines.forEach(out)
            nativeBridges.kotlinLines.forEach(out)
            if (context.platform == KotlinPlatform.JVM)
                out("private val loadLibrary = loadKonanLibrary(\"${context.libName}\")")
        }
    }
    private val printer = object : StubIrVisitor<StubContainer?, Unit> {

        override fun visitClass(element: ClassStub, owner: StubContainer?) {
            element.annotations.forEach {
                out(renderAnnotation(it))
            }
            val header = renderClassHeader(element)
            when {
                element is ClassStub.Simple && element.children.isEmpty() -> out(header)
                element is ClassStub.Companion && element.children.isEmpty() -> out(header)
                else -> block(header) {
                    if (element is ClassStub.Enum) {
                        emitEnumBody(element)
                    } else {
                        element.children.forEach {
                            emitEmptyLine()
                            it.accept(this, element)
                        }
                    }
                }
            }
        }

        override fun visitTypealias(element: TypealiasStub, owner: StubContainer?) {
            val alias = renderClassifierDeclaration(element.alias)
            val aliasee = renderStubType(element.aliasee)
            out("typealias $alias = $aliasee")
        }

        override fun visitFunction(element: FunctionStub, owner: StubContainer?) {
            if (element in bridgeBuilderResult.excludedStubs) return

            val header = run {
                val parameters = element.parameters.joinToString(prefix = "(", postfix = ")") { renderFunctionParameter(it) }
                val receiver = element.receiver?.let { renderFunctionReceiver(it) + "." } ?: ""
                val typeParameters = renderTypeParameters(element.typeParameters)
                val modality = renderMemberModality(element.modality, owner)
                "${modality}fun$typeParameters $receiver${element.name.asSimpleName()}$parameters: ${renderStubType(element.returnType)}"
            }
            if (!nativeBridges.isSupported(element)) {
                sequenceOf(
                        annotationForUnableToImport,
                        "$header = throw UnsupportedOperationException()"
                ).forEach(out)
                return
            }
            element.annotations.forEach {
                out(renderAnnotation(it))
            }
            when {
                element.external -> out("external $header")
                element.isOptionalObjCMethod() -> out("$header = optional()")
                owner != null && owner.isInterface -> out(header)
                else -> block(header) {
                    functionBridgeBodies.getValue(element).forEach(out)
                }
            }
        }

        override fun visitProperty(element: PropertyStub, owner: StubContainer?) {
            if (element in bridgeBuilderResult.excludedStubs) return

            val modality = renderMemberModality(element.modality, owner)
            val receiver = if (element.receiverType != null) "${renderStubType(element.receiverType)}." else ""
            val name = if (owner?.isTopLevelContainer == true) {
                getTopLevelPropertyDeclarationName(kotlinFile, element.name)
            } else {
                element.name.asSimpleName()
            }
            val header = "$receiver$name: ${renderStubType(element.type)}"

            if (element.kind is PropertyStub.Kind.Val && !nativeBridges.isSupported(element.kind.getter)
                    || element.kind is PropertyStub.Kind.Var && !nativeBridges.isSupported(element.kind.getter)) {
                out(annotationForUnableToImport)
                out("val $header")
                out("    get() = TODO()")
            } else {
                element.annotations.forEach {
                    out(renderAnnotation(it))
                }
                when (val kind = element.kind) {
                    is PropertyStub.Kind.Constant -> {
                        out("${modality}const val $header = ${renderValueUsage(kind.constant)}")
                    }
                    is PropertyStub.Kind.Val -> {
                        val shouldWriteInline = kind.getter is PropertyAccessor.Getter.SimpleGetter && kind.getter.constant != null
                        if (shouldWriteInline) {
                            out("${modality}val $header ${renderGetter(kind.getter)}")
                        } else {
                            out("${modality}val $header")
                            indent {
                                out(renderGetter(kind.getter))
                            }
                        }
                    }
                    is PropertyStub.Kind.Var -> {
                        val isSupported = nativeBridges.isSupported(kind.setter)
                        val variableKind = if (isSupported) "var" else "val"

                        out("$modality$variableKind $header")
                        indent {
                            out(renderGetter(kind.getter))
                            if (isSupported) {
                                out(renderSetter(kind.setter))
                            }
                        }
                    }
                }
            }
        }

        // Try to use the provided name. If failed, mangle it with underscore and try again:
        private tailrec fun getTopLevelPropertyDeclarationName(scope: KotlinScope, name: String): String =
                scope.declareProperty(name) ?: getTopLevelPropertyDeclarationName(scope, name + "_")

        override fun visitConstructor(constructorStub: ConstructorStub, owner: StubContainer?) {
            constructorStub.annotations.forEach {
                out(renderAnnotation(it))
            }
            val visibility = renderVisibilityModifier(constructorStub.visibility)
            out("${visibility}constructor(${constructorStub.parameters.joinToString { renderFunctionParameter(it) }}) {}")
        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, owner: StubContainer?) {

        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, owner: StubContainer?) {
            if (simpleStubContainer.meta.textAtStart.isNotEmpty()) {
                out(simpleStubContainer.meta.textAtStart)
            }
            simpleStubContainer.classes.forEach {
                emitEmptyLine()
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.functions.forEach {
                emitEmptyLine()
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.properties.forEach {
                emitEmptyLine()
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.typealiases.forEach {
                emitEmptyLine()
                it.accept(this, simpleStubContainer)
            }
            simpleStubContainer.simpleContainers.forEach {
                emitEmptyLine()
                it.accept(this, simpleStubContainer)
            }
            if (simpleStubContainer.meta.textAtEnd.isNotEmpty()) {
                out(simpleStubContainer.meta.textAtEnd)
            }
        }
    }

    // About method naming convention:
    // - "emit" prefix means that method will call `out` by itself.
    // - "render" prefix means that method returns string that should be emitted by caller.
    private fun emitEnumBody(enum: ClassStub.Enum) {

        enum.entries.forEach {
            out(renderEnumEntry(it) + ",")
        }

        val simpleKotlinName = enum.classifier.topLevelName.asSimpleName()
        val typeMirror = builderResult.bridgeGenerationComponents.enumToTypeMirror.getValue(enum)
        val baseKotlinType = typeMirror.argType.render(kotlinFile)
        val basePointedTypeName = typeMirror.pointedType.render(kotlinFile)

        out(";")
        emitEmptyLine()
        block("companion object") {
            enum.entries.forEach { entry ->
                entry.aliases.forEach {
                    out("val ${it.name.asSimpleName()} = ${entry.name.asSimpleName()}")
                }

            }
            emitEmptyLine()

            out("fun byValue(value: $baseKotlinType) = " +
                    "$simpleKotlinName.values().find { it.value == value }!!")
        }
        emitEmptyLine()
        block("class Var(rawPtr: NativePtr) : CEnumVar(rawPtr)") {
            out("companion object : Type($basePointedTypeName.size.toInt())")
            out("var value: $simpleKotlinName")
            out("    get() = byValue(this.reinterpret<$basePointedTypeName>().value)")
            out("    set(value) { this.reinterpret<$basePointedTypeName>().value = value.value }")
        }
    }

    private fun renderFunctionReceiver(receiver: ReceiverParameterStub): String {
        return renderStubType(receiver.type)
    }

    private fun renderFunctionParameter(parameter: FunctionParameterStub): String {
        val annotations = if (parameter.annotations.isEmpty())
            ""
        else
            parameter.annotations.joinToString(separator = " ") { renderAnnotation(it) } + " "
        val vararg = if (parameter.isVararg) "vararg " else ""
        return "$annotations$vararg${parameter.name.asSimpleName()}: ${renderStubType(parameter.type)}"
    }

    private fun renderMemberModality(modality: MemberStubModality, container: StubContainer?): String =
            if (container?.defaultMemberModality == modality) {
                ""
            } else
                when (modality) {
                    MemberStubModality.OVERRIDE -> "override "
                    MemberStubModality.OPEN -> "open "
                    MemberStubModality.FINAL -> "final "
                }

    private fun renderVisibilityModifier(visibilityModifier: VisibilityModifier) = when (visibilityModifier) {
        VisibilityModifier.PRIVATE -> "private "
        VisibilityModifier.PROTECTED -> "protected "
        VisibilityModifier.INTERNAL -> "internal "
        VisibilityModifier.PUBLIC -> ""
    }

    private fun renderClassHeader(classStub: ClassStub): String {
        val modality = when (classStub) {
            is ClassStub.Simple -> renderClassStubModality(classStub.modality)
            is ClassStub.Companion -> ""
            is ClassStub.Enum -> "enum class "
        }
        val className = when (classStub) {
            is ClassStub.Simple -> renderClassifierDeclaration(classStub.classifier)
            is ClassStub.Companion -> "companion object"
            is ClassStub.Enum -> renderClassifierDeclaration(classStub.classifier)
        }
        val constructorParams = when (classStub) {
            is ClassStub.Simple -> renderConstructorParams(classStub.constructorParameters)
            is ClassStub.Companion -> ""
            is ClassStub.Enum -> renderConstructorParams(classStub.constructorParameters)
        }
        val inheritance = mutableListOf<String>().apply {
            addIfNotNull(classStub.superClassInit?.let { renderSuperInit(it) })
            addAll(classStub.interfaces.map { renderStubType(it) })
        }.let { if (it.isNotEmpty()) " : ${it.joinToString()}" else "" }

        return "$modality$className$constructorParams$inheritance"
    }

    private fun renderClassifierDeclaration(classifier: Classifier): String =
            kotlinFile.declare(classifier)

    private fun renderClassStubModality(classStubModality: ClassStubModality): String = when (classStubModality) {
        ClassStubModality.INTERFACE -> "interface "
        ClassStubModality.OPEN -> "open class "
        ClassStubModality.ABSTRACT -> "abstract class "
        ClassStubModality.NONE -> "class "
    }

    private fun renderConstructorParams(parameters: List<ConstructorParameterStub>): String =
            if (parameters.isEmpty()) {
                ""
            } else {
                parameters.joinToString(prefix = "(", postfix = ")") { renderConstructorParameter(it) }
            }

    private fun renderConstructorParameter(parameterStub: ConstructorParameterStub): String {
        val prefix = when (parameterStub.qualifier) {
            is ConstructorParameterStub.Qualifier.VAL -> if (parameterStub.qualifier.overrides) "override val " else "val "
            is ConstructorParameterStub.Qualifier.VAR -> if (parameterStub.qualifier.overrides) "override var " else "var "
            ConstructorParameterStub.Qualifier.NONE -> ""
        }
        return "$prefix${parameterStub.name.asSimpleName()}: ${renderStubType(parameterStub.type)}"
    }

    private fun renderSuperInit(superClassInit: SuperClassInit): String {
        val parameters = superClassInit.arguments.joinToString(prefix = "(", postfix = ")") { renderValueUsage(it) }
        return "${renderStubType(superClassInit.type)}$parameters"
    }

    private fun renderStubType(stubType: StubType): String {
        val nullable = if (stubType.nullable) "?" else ""

        return when (stubType) {
            is ClassifierStubType -> {
                val classifier = kotlinFile.reference(stubType.classifier)
                val typeArguments = renderTypeArguments(stubType.typeArguments)
                "$classifier$typeArguments$nullable"
            }
            is FunctionalType -> buildString {
                if (stubType.nullable) append("(")

                append('(')
                stubType.parameterTypes.joinTo(this) { renderStubType(it) }
                append(") -> ")
                append(renderStubType(stubType.returnType))

                if (stubType.nullable) append(")?")
            }
            is TypeParameterType -> "${stubType.name}$nullable"
        }
    }

    private fun renderValueUsage(value: ValueStub): String = when (value) {
        is StringConstantStub -> value.value
        is IntegralConstantStub -> renderIntegralConstant(value)!!
        is DoubleConstantStub -> renderDoubleConstant(value)!!
        is GetConstructorParameter -> value.constructorParameterStub.name
    }

    private fun renderAnnotation(annotationStub: AnnotationStub): String = when (annotationStub) {
        AnnotationStub.ObjC.ConsumesReceiver -> "@CCall.ConsumesReceiver"
        AnnotationStub.ObjC.ReturnsRetained -> "@CCall.ReturnsRetained"
        is AnnotationStub.ObjC.Method -> {
            val stret = if (annotationStub.isStret) ", true" else ""
            val selector = annotationStub.selector.quoteAsKotlinLiteral()
            val encoding = annotationStub.encoding.quoteAsKotlinLiteral()
            "@ObjCMethod($selector, $encoding$stret)"
        }
        is AnnotationStub.ObjC.Factory -> {
            val stret = if (annotationStub.isStret) ", true" else ""
            val selector = annotationStub.selector.quoteAsKotlinLiteral()
            val encoding = annotationStub.encoding.quoteAsKotlinLiteral()
            "@ObjCFactory($selector, $encoding$stret)"
        }
        AnnotationStub.ObjC.Consumed ->
            "@CCall.Consumed"
        is AnnotationStub.ObjC.Constructor ->
            "@ObjCConstructor(${annotationStub.selector.quoteAsKotlinLiteral()}, ${annotationStub.designated})"
        is AnnotationStub.ObjC.ExternalClass -> {
            val protocolGetter = annotationStub.protocolGetter.quoteAsKotlinLiteral()
            val binaryName = annotationStub.binaryName.quoteAsKotlinLiteral()
            "@ExternalObjCClass" + when {
                annotationStub.protocolGetter.isEmpty() && annotationStub.binaryName.isEmpty() -> ""
                annotationStub.protocolGetter.isEmpty() -> "(\"\", $binaryName)"
                annotationStub.binaryName.isEmpty() -> "($protocolGetter)"
                else -> "($protocolGetter, $binaryName)"
            }
        }
        AnnotationStub.CCall.CString ->
            "@CCall.CString"
        AnnotationStub.CCall.WCString ->
            "@CCall.WCString"
        is AnnotationStub.CCall.Symbol ->
            "@CCall(${annotationStub.symbolName.quoteAsKotlinLiteral()})"
        is AnnotationStub.CStruct ->
            "@CStruct(${annotationStub.struct.quoteAsKotlinLiteral()})"
        is AnnotationStub.CNaturalStruct ->
            "@CNaturalStruct(${annotationStub.members.joinToString { it.name.quoteAsKotlinLiteral() }})"
        is AnnotationStub.CLength ->
            "@CLength(${annotationStub.length})"
        is AnnotationStub.Deprecated ->
            "@Deprecated(${annotationStub.message.quoteAsKotlinLiteral()}, " +
                    "ReplaceWith(${annotationStub.replaceWith.quoteAsKotlinLiteral()}), " +
                    "DeprecationLevel.ERROR)"
    }

    private fun renderEnumEntry(enumEntryStub: EnumEntryStub): String =
            "${enumEntryStub.name.asSimpleName()}(${renderValueUsage(enumEntryStub.constant)})"

    private fun renderGetter(accessor: PropertyAccessor.Getter): String {
        val annotations = accessor.annotations.joinToString(separator = "") { renderAnnotation(it) + " " }

        return annotations + if (accessor is PropertyAccessor.Getter.ExternalGetter) {
            "external get"
        } else {
            "get() = ${renderPropertyAccessorBody(accessor)}"
        }
    }

    private fun renderSetter(accessor: PropertyAccessor.Setter): String {
        val annotations = accessor.annotations.joinToString(separator = "") { renderAnnotation(it) + " " }
        return annotations + if (accessor is PropertyAccessor.Setter.ExternalSetter) {
            "external set"
        } else {
            "set(value) { ${renderPropertyAccessorBody(accessor)} }"
        }
    }

    private fun renderPropertyAccessorBody(accessor: PropertyAccessor): String = when (accessor) {
        is PropertyAccessor.Getter.SimpleGetter -> {
            when {
                accessor in propertyAccessorBridgeBodies -> propertyAccessorBridgeBodies.getValue(accessor)
                accessor.constant != null -> renderValueUsage(accessor.constant)
                else -> error("Bridge body for getter was not generated")
            }
        }

        is PropertyAccessor.Getter.ArrayMemberAt -> "arrayMemberAt(${accessor.offset})"

        is PropertyAccessor.Getter.MemberAt -> {
            val typeArguments = renderTypeArguments(accessor.typeArguments)
            val valueAccess = if (accessor.hasValueAccessor) ".value" else ""
            "memberAt$typeArguments(${accessor.offset})$valueAccess"
        }

        is PropertyAccessor.Getter.ReadBits -> {
            propertyAccessorBridgeBodies.getValue(accessor)
        }

        is PropertyAccessor.Setter.SimpleSetter -> when {
            accessor in propertyAccessorBridgeBodies -> propertyAccessorBridgeBodies.getValue(accessor)
            else -> error("Bridge body for setter was not generated")
        }

        is PropertyAccessor.Setter.MemberAt -> {
            if (accessor.typeArguments.isEmpty()) {
                error("Unexpected memberAt setter without type parameters!")
            } else {
                val typeArguments = renderTypeArguments(accessor.typeArguments)
                "memberAt$typeArguments(${accessor.offset}).value = value"
            }
        }

        is PropertyAccessor.Setter.WriteBits -> {
            propertyAccessorBridgeBodies.getValue(accessor)
        }

        is PropertyAccessor.Getter.InterpretPointed -> {
            val typeParameters = accessor.typeParameters.joinToString(prefix = "<", postfix = ">") { renderStubType(it) }
            val getAddressExpression = propertyAccessorBridgeBodies.getValue(accessor)
            "interpretPointed$typeParameters($getAddressExpression)"
        }
        is PropertyAccessor.Getter.ExternalGetter,
        is PropertyAccessor.Setter.ExternalSetter -> error("External property accessor shouldn't have a body!")
    }

    private fun renderIntegralConstant(integralValue: IntegralConstantStub): String? {
        val (value, size, isSigned) = integralValue
        return if (isSigned) {
            if (value == Long.MIN_VALUE) {
                return "${value + 1} - 1" // Workaround for "The value is out of range" compile error.
            }

            val narrowedValue: Number = when (size) {
                1 -> value.toByte()
                2 -> value.toShort()
                4 -> value.toInt()
                8 -> value
                else -> return null
            }

            narrowedValue.toString()
        } else {
            // Note: stub generator is built and run with different ABI versions,
            // so Kotlin unsigned types can't be used here currently.

            val narrowedValue: String = when (size) {
                1 -> (value and 0xFF).toString()
                2 -> (value and 0xFFFF).toString()
                4 -> (value and 0xFFFFFFFF).toString()
                8 -> java.lang.Long.toUnsignedString(value)
                else -> return null
            }

            "${narrowedValue}u"
        }
    }

    private fun renderDoubleConstant(doubleValue: DoubleConstantStub): String? {
        val (value, size) = doubleValue
        return when (size) {
            4 -> {
                val floatValue = value.toFloat()
                val bits = java.lang.Float.floatToRawIntBits(floatValue)
                "bitsToFloat($bits) /* == $floatValue */"
            }
            8 -> {
                val bits = java.lang.Double.doubleToRawLongBits(value)
                "bitsToDouble($bits) /* == $value */"
            }
            else -> null
        }
    }

    private fun renderTypeArguments(typeArguments: List<TypeArgument>) = if (typeArguments.isNotEmpty()) {
        typeArguments.joinToString(", ", "<", ">") { renderTypeArgument(it) }
    } else {
        ""
    }

    private fun renderTypeArgument(typeArgument: TypeArgument) = when (typeArgument) {
        is TypeArgumentStub -> {
            val variance = when (typeArgument.variance) {
                TypeArgument.Variance.INVARIANT -> ""
                TypeArgument.Variance.IN -> "in "
                TypeArgument.Variance.OUT -> "out "
            }
            "$variance${renderStubType(typeArgument.type)}"
        }
        TypeArgument.StarProjection -> "*"
        else -> error("Unexpected type argument: $typeArgument")
    }

    private fun renderTypeParameters(typeParameters: List<TypeParameterStub>) = if (typeParameters.isNotEmpty()) {
        typeParameters.joinToString(", ", " <", ">") { renderTypeParameter(it) }
    } else {
        ""
    }

    private fun renderTypeParameter(typeParameterStub: TypeParameterStub): String {
        val name = typeParameterStub.name
        return typeParameterStub.upperBound?.let {
            "$name : ${renderStubType(it)}"
        } ?: name
    }
}