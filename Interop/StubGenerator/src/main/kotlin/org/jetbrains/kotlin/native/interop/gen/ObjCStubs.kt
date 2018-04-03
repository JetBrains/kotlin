/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.StubGenerator
import org.jetbrains.kotlin.native.interop.indexer.*

private fun ObjCMethod.getKotlinParameterNames(): List<String> {
    val selectorParts = this.selector.split(":")

    val result = mutableListOf<String>()

    // The names of all parameters except first must depend only on the selector:
    this.parameters.forEachIndexed { index, _ ->
        if (index > 0) {
            var name = selectorParts[index]
            if (name.isEmpty()) {
                name = "_$index"
            }

            while (name in result) {
                name = "_$name"
            }

            result.add(name)
        }
    }

    this.parameters.firstOrNull()?.let {
        var name = it.name ?: ""
        if (name.isEmpty()) {
            name = "arg"
        }
        while (name in result) {
            name = "_$name"
        }
        result.add(0, name)
    }

    return result
}

class ObjCMethodStub(stubGenerator: StubGenerator,
                     val method: ObjCMethod,
                     private val container: ObjCContainer) : KotlinStub, NativeBacked {

    override fun generate(context: StubGenerationContext): Sequence<String> =
            if (context.nativeBridges.isSupported(this)) {
                val result = mutableListOf<String>()
                result.add("@ObjCMethod".applyToStrings(method.selector, bridgeName))
                result.add(header)

                if (method.isInit && container is ObjCClass) {
                    result.add("")
                    result.add("@ObjCConstructor".applyToStrings(method.selector))
                    result.add("constructor($joinedKotlinParameters) {}")
                }

                context.addTopLevelDeclaration(
                        listOf("@konan.internal.ExportForCompiler",
                                "@ObjCBridge".applyToStrings(method.selector, method.encoding, implementationTemplate))
                                + block(bridgeHeader, bodyLines)
                )

                result.asSequence()
            } else {
                sequenceOf(
                        annotationForUnableToImport,
                        header
                )
            }

    private val bodyLines: List<String>
    private val joinedKotlinParameters: String
    private val header: String
    private val implementationTemplate: String
    internal val bridgeName: String
    private val bridgeHeader: String

    init {
        val bodyGenerator = KotlinCodeBuilder(scope = stubGenerator.kotlinFile)

        val kotlinParameters = mutableListOf<Pair<String, KotlinType>>()
        val kotlinObjCBridgeParameters = mutableListOf<Pair<String, KotlinType>>()
        val nativeBridgeArguments = mutableListOf<TypedKotlinValue>()

        val kniReceiverParameter = "kniR"
        val kniSuperClassParameter = "kniSC"

        val voidPtr = PointerType(VoidType)

        val returnType = method.getReturnType(container.classOrProtocol)

        val messengerGetter = if (returnType.isLargeOrUnaligned()) "getMessengerLU" else "getMessenger"

        kotlinObjCBridgeParameters.add(kniSuperClassParameter to KotlinTypes.nativePtr)
        nativeBridgeArguments.add(TypedKotlinValue(voidPtr, "$messengerGetter($kniSuperClassParameter)"))

        if (method.nsConsumesSelf) {
            // TODO: do this later due to possible exceptions
            bodyGenerator.out("objc_retain($kniReceiverParameter)")
        }

        kotlinObjCBridgeParameters.add(kniReceiverParameter to KotlinTypes.nativePtr)
        nativeBridgeArguments.add(
                TypedKotlinValue(voidPtr,
                        "getReceiverOrSuper($kniReceiverParameter, $kniSuperClassParameter)"))

        val kotlinParameterNames = method.getKotlinParameterNames()

        method.parameters.forEachIndexed { index, it ->
            val name = kotlinParameterNames[index]

            val kotlinType = stubGenerator.mirror(it.type).argType
            kotlinParameters.add(name to kotlinType)

            kotlinObjCBridgeParameters.add(name to kotlinType)
            nativeBridgeArguments.add(TypedKotlinValue(it.type, name.asSimpleName()))
        }

        val kotlinReturnType = if (returnType.unwrapTypedefs() is VoidType) {
            KotlinTypes.unit
        } else {
            stubGenerator.mirror(returnType).argType
        }.render(stubGenerator.kotlinFile)

        val result = stubGenerator.mappingBridgeGenerator.kotlinToNative(
                bodyGenerator,
                this@ObjCMethodStub,
                returnType,
                nativeBridgeArguments
        ) { nativeValues ->
            val selector = "@selector(${method.selector})"
            val messengerParameterTypes = mutableListOf<String>()
            messengerParameterTypes.add("void*")
            messengerParameterTypes.add("SEL")
            method.parameters.forEach {
                messengerParameterTypes.add(it.getTypeStringRepresentation())
            }

            val messengerReturnType = returnType.getStringRepresentation()

            val messengerType =
                    "$messengerReturnType (* ${method.cAttributes}) (${messengerParameterTypes.joinToString()})"

            val messenger = "(($messengerType) ${nativeValues.first()})"

            val messengerArguments = listOf(nativeValues[1]) + selector + nativeValues.drop(2)

            "$messenger(${messengerArguments.joinToString()})"
        }
        bodyGenerator.out("return $result")

        this.implementationTemplate = genImplementationTemplate(stubGenerator)
        this.bodyLines = bodyGenerator.build()

        bridgeName = "objcKniBridge${stubGenerator.nextUniqueId()}"

        this.bridgeHeader = buildString {
            append("internal fun ")
            append(bridgeName)
            append('(')
            kotlinObjCBridgeParameters.joinTo(this) {
                "${it.first.asSimpleName()}: ${it.second.render(stubGenerator.kotlinFile)}"
            }
            append("): ")
            append(kotlinReturnType)
        }

        this.joinedKotlinParameters = kotlinParameters.joinToString {
            "${it.first.asSimpleName()}: ${it.second.render(stubGenerator.kotlinFile)}"
        }

        this.header = buildString {
            if (container !is ObjCProtocol) append("external ")
            val modality = when (container) {
                is ObjCClassOrProtocol -> if (method.isOverride(container)) {
                    "override "
                } else when (container) {
                    is ObjCClass -> "open "
                    is ObjCProtocol -> ""
                }
                is ObjCCategory -> ""
            }
            append(modality)

            append("fun ")
            if (container is ObjCCategory) {
                val receiverType = stubGenerator.declarationMapper
                        .getKotlinClassFor(container.clazz, isMeta = method.isClass).type
                        .render(stubGenerator.kotlinFile)

                append(receiverType)
                append('.')
            }
            append("${method.kotlinName.asSimpleName()}($joinedKotlinParameters): $kotlinReturnType")

            if (container is ObjCProtocol && method.isOptional) append(" = optional()")
        }
    }

    private fun genImplementationTemplate(stubGenerator: StubGenerator): String = when (container) {
        is ObjCClassOrProtocol -> {
            val codeBuilder = NativeCodeBuilder(stubGenerator.simpleBridgeGenerator.topLevelNativeScope)

            val result = codeBuilder.genMethodImp(stubGenerator, this, method, container)
            stubGenerator.simpleBridgeGenerator.insertNativeBridge(this, emptyList(), codeBuilder.lines)
            result
        }
        is ObjCCategory -> ""
    }
}

private val ObjCContainer.classOrProtocol: ObjCClassOrProtocol
    get() = when (this) {
        is ObjCClassOrProtocol -> this
        is ObjCCategory -> this.clazz
    }

private fun Type.isLargeOrUnaligned(): Boolean {
    val unwrappedType = this.unwrapTypedefs()
    return when (unwrappedType) {
        is RecordType -> unwrappedType.decl.def!!.size > 16 || this.hasUnalignedMembers()
        else -> false
    }
}

private fun Type.hasUnalignedMembers(): Boolean = when (this) {
    is Typedef -> this.def.aliased.hasUnalignedMembers()
    is RecordType -> this.decl.def!!.let { def ->
        def.fields.any {
            !it.isAligned ||
                    // Check members of fields too:
                    it.type.hasUnalignedMembers()
        }
    }
    is ArrayType -> this.elemType.hasUnalignedMembers()
    else -> false

// TODO: should the recursive checks be made in indexer when computing `hasUnalignedFields`?
}

private val ObjCMethod.kotlinName: String get() = selector.split(":").first()

private val ObjCClassOrProtocol.protocolsWithSupers: Sequence<ObjCProtocol>
    get() = this.protocols.asSequence().flatMap { sequenceOf(it) + it.protocolsWithSupers }

private val ObjCClassOrProtocol.immediateSuperTypes: Sequence<ObjCClassOrProtocol>
    get() {
        val baseClass = (this as? ObjCClass)?.baseClass
        if (baseClass != null) {
            return sequenceOf(baseClass) + this.protocols.asSequence()
        }

        return this.protocols.asSequence()
    }

private val ObjCClassOrProtocol.selfAndSuperTypes: Sequence<ObjCClassOrProtocol>
    get() = sequenceOf(this) + this.superTypes

private val ObjCClassOrProtocol.superTypes: Sequence<ObjCClassOrProtocol>
    get() = this.immediateSuperTypes.flatMap { it.selfAndSuperTypes }.distinct()

private fun ObjCClassOrProtocol.declaredMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.methods.asSequence().filter { it.isClass == isClass }

private fun ObjCClassOrProtocol.inheritedMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.superTypes.flatMap { it.declaredMethods(isClass) }.distinctBy { it.selector }

private fun ObjCClassOrProtocol.methodsWithInherited(isClass: Boolean): Sequence<ObjCMethod> =
        this.selfAndSuperTypes.flatMap { it.declaredMethods(isClass) }.distinctBy { it.selector }

private fun ObjCMethod.isOverride(container: ObjCClassOrProtocol): Boolean =
        container.superTypes.any { superType -> superType.methods.any(this::replaces) }

abstract class ObjCContainerStub(stubGenerator: StubGenerator,
                                 private val container: ObjCClassOrProtocol,
                                 protected val metaContainerStub: ObjCContainerStub?
) : KotlinStub {

    private val isMeta: Boolean get() = metaContainerStub == null

    private val methods: List<ObjCMethod>
    private val properties: List<ObjCProperty>

    private val protocolGetter: String?

    init {
        val superMethods = container.inheritedMethods(isMeta)

        // Add all methods declared in the class or protocol:
        var methods = container.declaredMethods(isMeta)

        // Exclude those which are identically declared in super types:
        methods -= superMethods

        // Add some special methods from super types:
        methods += superMethods.filter { it.returnsInstancetype() || it.isInit }

        // Add methods from adopted protocols that must be implemented according to Kotlin rules:
        if (container is ObjCClass) {
            methods += container.protocolsWithSupers.flatMap { it.declaredMethods(isMeta) }.filter { !it.isOptional }
        }

        // Add methods inherited from multiple supertypes that must be defined according to Kotlin rules:
        methods += container.immediateSuperTypes
                .flatMap { superType ->
                    val methodsWithInherited = superType.methodsWithInherited(isMeta)
                    // Select only those which are represented as non-abstract in Kotlin:
                    when (superType) {
                        is ObjCClass -> methodsWithInherited
                        is ObjCProtocol -> methodsWithInherited.filter { it.isOptional }
                    }
                }
                .groupBy { it.selector }
                .mapNotNull { (_, inheritedMethods) -> if (inheritedMethods.size > 1) inheritedMethods.first() else null }

        this.methods = methods.distinctBy { it.selector }.toList()

        this.properties = container.properties.filter { property ->
            property.getter.isClass == isMeta &&
                    // Select only properties that don't override anything:
                    superMethods.none { property.getter.replaces(it) || property.setter?.replaces(it) ?: false }
        }
    }

    private val methodToStub = methods.map {
        it to ObjCMethodStub(stubGenerator, it, container)
    }.toMap()

    private val methodStubs get() = methodToStub.values

    val propertyStubs = properties.map {
        createObjCPropertyStub(stubGenerator, it, container, this.methodToStub)
    }

    private val classHeader: String

    init {
        val supers = mutableListOf<KotlinType>()

        if (container is ObjCClass) {
            val baseClass = container.baseClass
            val baseClassifier = if (baseClass != null) {
                stubGenerator.declarationMapper.getKotlinClassFor(baseClass, isMeta)
            } else {
                if (isMeta) KotlinTypes.objCObjectBaseMeta else KotlinTypes.objCObjectBase
            }

            supers.add(baseClassifier.type)
        }
        container.protocols.forEach {
            supers.add(stubGenerator.declarationMapper.getKotlinClassFor(it, isMeta).type)
        }

        if (supers.isEmpty()) {
            assert(container is ObjCProtocol)
            val classifier = if (isMeta) KotlinTypes.objCObjectMeta else KotlinTypes.objCObject
            supers.add(classifier.type)
        }

        val keywords = when (container) {
            is ObjCClass -> "open class"
            is ObjCProtocol -> "interface"
        }

        val supersString = supers.joinToString { it.render(stubGenerator.kotlinFile) }
        val classifier = stubGenerator.declarationMapper.getKotlinClassFor(container, isMeta)
        val name = stubGenerator.kotlinFile.declare(classifier)

        val externalObjCClassAnnotationName = "@ExternalObjCClass"
        val externalObjCClassAnnotation: String

        if (container is ObjCProtocol) {
            protocolGetter = if (metaContainerStub != null) {
                metaContainerStub.protocolGetter!!
            } else {
                val nativeBacked = object : NativeBacked {}
                // TODO: handle the case when protocol getter stub can't be compiled.
                genProtocolGetter(stubGenerator, nativeBacked, container)
            }

            externalObjCClassAnnotation = externalObjCClassAnnotationName.applyToStrings(protocolGetter)
        } else {
            protocolGetter = null
            externalObjCClassAnnotation = externalObjCClassAnnotationName
        }

        this.classHeader = "$externalObjCClassAnnotation $keywords $name : $supersString"
    }

    open fun generateBody(context: StubGenerationContext): Sequence<String> {
        var result = (propertyStubs.asSequence() + methodStubs.asSequence())
                .flatMap { sequenceOf("") + it.generate(context) }

        if (container is ObjCClass && methodStubs.none {
            it.method.isInit && it.method.parameters.isEmpty() && context.nativeBridges.isSupported(it)
        }) {
            // Always generate default constructor.
            // If it is not produced for an init method, then include it manually:
            result += sequenceOf("", "protected constructor() {}")
        }

        return result
    }

    override fun generate(context: StubGenerationContext): Sequence<String> = block(classHeader, generateBody(context))
}

open class ObjCClassOrProtocolStub(
        stubGenerator: StubGenerator,
        private val container: ObjCClassOrProtocol
) : ObjCContainerStub(
        stubGenerator,
        container,
        metaContainerStub = object : ObjCContainerStub(stubGenerator, container, metaContainerStub = null) {}
) {
    override fun generate(context: StubGenerationContext) =
            metaContainerStub!!.generate(context) + "" + super.generate(context)
}

class ObjCProtocolStub(stubGenerator: StubGenerator, protocol: ObjCProtocol) :
        ObjCClassOrProtocolStub(stubGenerator, protocol)

class ObjCClassStub(private val stubGenerator: StubGenerator, private val clazz: ObjCClass) :
        ObjCClassOrProtocolStub(stubGenerator, clazz) {

    override fun generateBody(context: StubGenerationContext): Sequence<String> {
        val companionSuper = stubGenerator.declarationMapper
                .getKotlinClassFor(clazz, isMeta = true).type
                .render(stubGenerator.kotlinFile)
        return sequenceOf( "companion object : $companionSuper() {}") +
                super.generateBody(context)
    }
}

class ObjCCategoryStub(
        private val stubGenerator: StubGenerator, private val category: ObjCCategory
) : KotlinStub {

    // TODO: consider removing members that are also present in the class or its supertypes.

    private val methodToStub = category.methods.map {
        it to ObjCMethodStub(stubGenerator, it, category)
    }.toMap()

    private val methodStubs get() = methodToStub.values

    private val propertyStubs = category.properties.map {
        createObjCPropertyStub(stubGenerator, it, category, methodToStub)
    }

    override fun generate(context: StubGenerationContext): Sequence<String> {
        val description = "${category.clazz.name} (${category.name})"
        return sequenceOf("// @interface $description") +
                propertyStubs.asSequence().flatMap { sequenceOf("") + it.generate(context) } +
                methodStubs.asSequence().flatMap { sequenceOf("") + it.generate(context) } +
                sequenceOf("// @end; // $description")
    }
}

private fun createObjCPropertyStub(
        stubGenerator: StubGenerator,
        property: ObjCProperty,
        container: ObjCContainer,
        methodToStub: Map<ObjCMethod, ObjCMethodStub>
): ObjCPropertyStub {
    // Note: the code below assumes that if the property is generated,
    // then its accessors are also generated as explicit methods.
    val getterStub = methodToStub[property.getter]!!
    val setterStub = property.setter?.let { methodToStub[it]!! }
    return ObjCPropertyStub(stubGenerator, property, container, getterStub, setterStub)
}

class ObjCPropertyStub(
        val stubGenerator: StubGenerator, val property: ObjCProperty, val container: ObjCContainer,
        val getterStub: ObjCMethodStub, val setterStub: ObjCMethodStub?
) : KotlinStub {

    override fun generate(context: StubGenerationContext): Sequence<String> {
        val type = property.getType(container.classOrProtocol)

        val kotlinType = stubGenerator.mirror(type).argType.render(stubGenerator.kotlinFile)

        val kind = if (property.setter == null) "val" else "var"
        val modifiers = if (container is ObjCProtocol) "final " else ""
        val receiver = when (container) {
            is ObjCClassOrProtocol -> ""
            is ObjCCategory -> stubGenerator.declarationMapper
                    .getKotlinClassFor(container.clazz, isMeta = property.getter.isClass).type
                    .render(stubGenerator.kotlinFile) + "."
        }
        val result = mutableListOf(
                "$modifiers$kind $receiver${property.name.asSimpleName()}: $kotlinType",
                "    get() = ${getterStub.bridgeName}(nativeNullPtr, this.rawPtr())"
        )

        property.setter?.let {
            result.add("    set(value) = ${setterStub!!.bridgeName}(nativeNullPtr, this.rawPtr(), value)")
        }

        return result.asSequence()
    }

}

fun ObjCClassOrProtocol.kotlinClassName(isMeta: Boolean): String {
    val baseClassName = when (this) {
        is ObjCClass -> this.name
        is ObjCProtocol -> "${this.name}Protocol"
    }

    return if (isMeta) "${baseClassName}Meta" else baseClassName
}

private fun Parameter.getTypeStringRepresentation() =
        (if (this.nsConsumed) "__attribute__((ns_consumed)) " else "") + type.getStringRepresentation()

private fun ObjCMethod.getSelfTypeStringRepresentation() = if (this.nsConsumesSelf) {
    "__attribute__((ns_consumed)) id"
} else {
    "id"
}

val ObjCMethod.cAttributes get() = if (this.nsReturnsRetained) {
    "__attribute__((ns_returns_retained)) "
} else {
    ""
}

private fun NativeCodeBuilder.genMethodImp(
        stubGenerator: StubGenerator,
        nativeBacked: NativeBacked,
        method: ObjCMethod,
        container: ObjCClassOrProtocol
): String {

    val returnType = method.getReturnType(container)
    val cReturnType = returnType.getStringRepresentation()

    val bridgeArguments = mutableListOf<TypedNativeValue>()

    val parameters = mutableListOf<Pair<String, String>>()

    parameters.add("self" to method.getSelfTypeStringRepresentation())

    val receiverType = ObjCIdType(ObjCPointer.Nullability.NonNull, protocols = emptyList())
    bridgeArguments.add(TypedNativeValue(receiverType, "self"))

    parameters.add("_cmd" to "SEL")

    method.parameters.forEachIndexed { index, parameter ->
        val name = "p$index"
        parameters.add(name to parameter.getTypeStringRepresentation())
        bridgeArguments.add(TypedNativeValue(parameter.type, name))
    }

    val functionName = "knimi_" + stubGenerator.pkgName.replace('.', '_') + stubGenerator.nextUniqueId()
    val functionAttr = method.cAttributes

    out("$cReturnType $functionName(${parameters.joinToString { it.second + " " + it.first }}) $functionAttr{")

    val callExpr = stubGenerator.mappingBridgeGenerator.nativeToKotlin(
            this,
            nativeBacked,
            returnType,
            bridgeArguments
    ) { kotlinValues ->

        val kotlinReceiverType = stubGenerator.declarationMapper
                .getKotlinClassFor(container, isMeta = method.isClass)
                .type.render(stubGenerator.kotlinFile)

        val kotlinRawReceiver = kotlinValues.first()
        val kotlinReceiver = "$kotlinRawReceiver.uncheckedCast<$kotlinReceiverType>()"

        val namedArguments = kotlinValues.drop(1).zip(method.getKotlinParameterNames()) { value, name ->
            "${name.asSimpleName()} = $value"
        }

        "${kotlinReceiver}.${method.kotlinName.asSimpleName()}(${namedArguments.joinToString()})"
    }

    if (returnType.unwrapTypedefs() is VoidType) {
        out("    $callExpr;")
    } else {
        out("    return $callExpr;")
    }

    out("}")

    return functionName
}

private fun genProtocolGetter(
        stubGenerator: StubGenerator,
        nativeBacked: NativeBacked,
        protocol: ObjCProtocol
): String {
    val functionName = "kniprot_" + stubGenerator.pkgName.replace('.', '_') + stubGenerator.nextUniqueId()

    val builder = NativeCodeBuilder(stubGenerator.simpleBridgeGenerator.topLevelNativeScope)

    with(builder) {
        out("Protocol* $functionName() {")
        out("    return @protocol(${protocol.name});")
        out("}")
    }

    stubGenerator.simpleBridgeGenerator.insertNativeBridge(nativeBacked, emptyList(), builder.lines)

    return functionName
}
