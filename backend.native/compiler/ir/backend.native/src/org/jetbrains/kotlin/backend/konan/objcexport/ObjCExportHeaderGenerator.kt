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

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.util.report
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addIfNotNull

internal class ObjCExportHeaderGenerator(val context: Context) {
    val mapper: ObjCExportMapper = object : ObjCExportMapper() {
        override fun getCategoryMembersFor(descriptor: ClassDescriptor) =
                extensions[descriptor].orEmpty()

        override fun isSpecialMapped(descriptor: ClassDescriptor): Boolean {
            // TODO: this method duplicates some of the [mapReferenceType] logic.
            return descriptor == context.builtIns.any ||
                    descriptor.getAllSuperClassifiers().any { it in customTypeMappers }
        }
    }

    val namer = ObjCExportNamer(context, mapper)

    val generatedClasses = mutableSetOf<ClassDescriptor>()
    val topLevel = mutableMapOf<FqName, MutableList<CallableMemberDescriptor>>()

    val customTypeMappers: Map<ClassDescriptor, CustomTypeMapper> = with (context.builtIns) {
        val result = mutableListOf<CustomTypeMapper>()

        val generator = this@ObjCExportHeaderGenerator

        result += CustomTypeMapper.Collection(generator, list, "NSArray")
        result += CustomTypeMapper.Collection(generator, mutableList, "NSMutableArray")
        result += CustomTypeMapper.Collection(generator, set, "NSSet")
        result += CustomTypeMapper.Collection(generator, mutableSet, namer.mutableSetName)
        result += CustomTypeMapper.Collection(generator, map, "NSDictionary")
        result += CustomTypeMapper.Collection(generator, mutableMap, namer.mutableMapName)

        for (descriptor in listOf(boolean, char, byte, short, int, long, float, double)) {
            // TODO: Kotlin code doesn't have any checkcasts on unboxing,
            // so it is possible that it expects boxed number of other type and unboxes it incorrectly.
            // TODO: NSNumber seem to have different equality semantics.
            result += CustomTypeMapper.Simple(descriptor, "NSNumber")
        }

        result += CustomTypeMapper.Simple(string, "NSString")

        (0 .. mapper.maxFunctionTypeParameterCount).forEach {
            result += CustomTypeMapper.Function(generator, it)
        }

        result.associateBy { it.mappedClassDescriptor }
    }

    val hiddenTypes: Set<ClassDescriptor> = run {
        val customMappedTypes = customTypeMappers.keys

        customMappedTypes
                .flatMap { it.getAllSuperClassifiers().toList() }
                .map { it as ClassDescriptor }
                .toSet() - customMappedTypes
    }

    private val kotlinAnyName = namer.kotlinAnyName

    private val stubs = mutableListOf<Stub>()
    private val classOrInterfaceToName = mutableMapOf<ClassDescriptor, String>()

    internal val classForwardDeclarations = mutableSetOf<String>()
    internal val protocolForwardDeclarations = mutableSetOf<String>()

    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    fun translateModule() {
        // TODO: make the translation order stable
        // to stabilize name mangling.

        val packageFragments = context.moduleDescriptor.getPackageFragments()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val classDescriptor = mapper.getClassIfCategory(it)
                        if (classDescriptor != null) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        } else {
                            topLevel.getOrPut(packageFragment.fqName, { mutableListOf() }) += it
                        }
                    }

        }

        fun MemberScope.translateClasses(): Unit = this.getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .filter { mapper.shouldBeExposed(it) }
                .forEach {
                    if (it.isInterface) {
                        translateInterface(it)
                    } else {
                        translateClass(it)
                    }

                    it.unsubstitutedMemberScope.translateClasses()
                }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().translateClasses()
        }

        extensions.forEach { classDescriptor, declarations ->
            translateExtensions(classDescriptor, declarations)
        }

        topLevel.forEach { packageFqName, declarations ->
            translateTopLevel(packageFqName, declarations)
        }

        while (extraClassesToTranslate.isNotEmpty()) {
            val descriptor = extraClassesToTranslate.first()
            extraClassesToTranslate -= descriptor
            if (descriptor.isInterface) {
                translateInterface(descriptor)
            } else {
                translateClass(descriptor)
            }
        }
    }

    fun translateClassName(descriptor: ClassDescriptor): String = classOrInterfaceToName.getOrPut(descriptor) {
        assert(mapper.shouldBeExposed(descriptor))
        val forwardDeclarations = if (descriptor.isInterface) protocolForwardDeclarations else classForwardDeclarations

        namer.getClassOrProtocolName(descriptor).also { forwardDeclarations += it }
    }

    private fun translateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return

        val name = translateClassName(descriptor)

        stubs.addBuiltBy {
            +"@protocol $name${descriptor.superProtocolsClause}"
            +"@required"

            translateClassOrInterfaceMembers(descriptor)

            +"@end;"
        }
    }

    private val ClassDescriptor.superProtocolsClause: String get() {
        val interfaces = this.getSuperInterfaces().filter { mapper.shouldBeExposed(it) }
        return if (interfaces.isEmpty()) {
            ""
        } else buildString {
            append(" <")
            interfaces.joinTo(this) {
                translateInterface(it)
                translateClassName(it)
            }
            append(">")
        }
    }

    private fun translateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        translateClass(classDescriptor)

        stubs.addBuiltBy {
            +"@interface ${translateClassName(classDescriptor)} (Extensions)"

            translateMembers(declarations)

            +"@end;"
        }
    }

    private fun translateTopLevel(packageFqName: FqName, declarations: List<CallableMemberDescriptor>) {
        val name = namer.getPackageName(packageFqName)
        stubs.addBuiltBy {
            +"__attribute__((objc_subclassing_restricted))"
            +"@interface $name : ${namer.kotlinAnyName}" // TODO: stop inheriting KotlinBase.

            translateMembers(declarations)

            +"@end;"
        }
    }

    private fun translateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return

        val name = translateClassName(descriptor)
        val superClass = descriptor.getSuperClassNotAny()

        val superName = if (superClass == null) {
            kotlinAnyName
        } else {
            translateClass(superClass)
            translateClassName(superClass)
        }

        stubs.addBuiltBy {
            if (descriptor.isFinalOrEnum) {
                +"__attribute__((objc_subclassing_restricted))"
            }

            +"@interface $name : $superName${descriptor.superProtocolsClause}"

            val presentConstructors = mutableSetOf<String>()

            descriptor.constructors.filter { mapper.shouldBeExposed(it) }.forEach {
                val selector = getSelector(it)
                if (!descriptor.isArray) presentConstructors += selector

                +"${getSignature(it, it)};"
                if (selector == "init") {
                    +"+ (instancetype)new OBJC_SWIFT_UNAVAILABLE(\"use object initializers instead\");"
                }
                +""
            }

            if (descriptor.isArray || descriptor.kind == ClassKind.OBJECT || descriptor.kind == ClassKind.ENUM_CLASS) {
                +"+(instancetype)alloc __attribute__((unavailable));"
                +"+(instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));"
                +""
            }

            // TODO: consider adding exception-throwing impls for these.
            when (descriptor.kind) {
                ClassKind.OBJECT -> {
                    +"+(instancetype)${namer.getObjectInstanceSelector(descriptor)} NS_SWIFT_NAME(init());"
                    +""
                }
                ClassKind.ENUM_CLASS -> {
                    val type = mapType(descriptor.defaultType, ReferenceBridge)

                    descriptor.enumEntries.forEach {
                        val entryName = namer.getEnumEntrySelector(it)
                        +"@property (class, readonly) ${type.render(entryName)};"
                    }
                    +""
                }
                else -> {
                    // Nothing special.
                }
            }

            // Hide "unimplemented" super constructors:
            superClass?.constructors?.filter { mapper.shouldBeExposed(it) }?.forEach {
                val selector = getSelector(it)
                if (selector !in presentConstructors) {
                    +"${getSignature(it, it)} __attribute__((unavailable));"
                    if (selector == "init") {
                        +"+(instancetype) new __attribute__((unavailable));"
                    }

                    +""
                    // TODO: consider adding exception-throwing impls for these.
                }
            }

            translateClassOrInterfaceMembers(descriptor)

            +"@end;"
        }
    }

    private fun StubBuilder.translateClassOrInterfaceMembers(descriptor: ClassDescriptor) {
        val members = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { mapper.shouldBeExposed(it) }

        translateMembers(members)
    }

    private fun StubBuilder.translateMembers(members: List<CallableMemberDescriptor>) {
        // TODO: add some marks about modality.

        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.forEach {
            when (it) {
                is FunctionDescriptor -> methods += it
                is PropertyDescriptor -> if (mapper.isObjCProperty(it)) {
                    properties += it
                } else {
                    methods.addIfNotNull(it.getter)
                    methods.addIfNotNull(it.setter)
                }
                else -> error(it)
            }
        }

        methods.forEach { method ->
            val superSignatures = method.overriddenDescriptors
                    .filter { mapper.shouldBeExposed(it) }
                    .flatMap { getSignatures(it.original) }
                    .toSet()

            (getSignatures(method) - superSignatures).forEach {
                +"$it;"
            }
        }

        properties.forEach { property ->
            val superSignatures = property.overriddenDescriptors
                    .filter { mapper.shouldBeExposed(it) }
                    .flatMap { getSignatures(it.original) }
                    .toSet()

            getSignatures(property).filter { it !in superSignatures }.forEach {
                +"$it;"
            }
        }
    }

    private val methodToSignatures = mutableMapOf<FunctionDescriptor, Set<String>>()

    private fun getSignatures(method: FunctionDescriptor) = methodToSignatures.getOrPut(method) {
        mapper.getBaseMethods(method).distinctBy { namer.getSelector(it) }.map { base ->
            getSignature(method, base)
        }.toSet()
    }

    private val propertyToSignatures = mutableMapOf<PropertyDescriptor, Set<String>>()

    private fun getSignatures(property: PropertyDescriptor) = propertyToSignatures.getOrPut(property) {
        mapper.getBaseProperties(property).distinctBy { namer.getName(it) }.map { base ->
            getSignature(property, base)
        }.toSet()
    }

    // TODO: consider checking that signatures for bases with same selector/name are equal.

    fun getSelector(method: FunctionDescriptor): String {
        return namer.getSelector(method)
    }

    private fun getSignature(property: PropertyDescriptor, baseProperty: PropertyDescriptor) = buildString {
        assert(mapper.isBaseProperty(baseProperty))
        assert(mapper.isObjCProperty(baseProperty))

        val getterBridge = mapper.bridgeMethod(baseProperty.getter!!)
        val type = mapReturnType(getterBridge.returnBridge, property.getter!!)
        val name = namer.getName(baseProperty)

        append("@property ")

        val attributes = mutableListOf<String>()

        if (!getterBridge.isInstance) {
            attributes += "class"
        }

        val getterSelector = getSelector(baseProperty.getter!!)
        if (getterSelector != name) {
            attributes += "getter=$getterSelector"
        }

        val propertySetter = property.setter
        if (propertySetter != null && mapper.shouldBeExposed(propertySetter)) {
            val setterSelector = mapper.getBaseMethods(propertySetter).map { namer.getSelector(it) }.distinct().single()
            if (setterSelector != "set" + name.capitalize() + ":") {
                attributes += "setter=$setterSelector"
            }
        } else {
            attributes += "readonly"
        }

        if (attributes.isNotEmpty()) {
            attributes.joinTo(this, prefix = "(", postfix = ") ")
        }

        append(type.render(name))
    }

    private fun getSignature(method: FunctionDescriptor, baseMethod: FunctionDescriptor) = buildString {
        assert(mapper.isBaseMethod(baseMethod))
        val methodBridge = mapper.bridgeMethod(baseMethod)

        exportThrownFromThisAndOverridden(method)

        val selectorParts = getSelector(baseMethod).split(':')

        if (methodBridge.isInstance) {
            append("-")
        } else {
            append("+")
        }

        append("(")
        append(mapReturnType(methodBridge.returnBridge, method).render())
        append(")")

        val valueParametersAssociated = methodBridge.valueParametersAssociated(method)

        val valueParameterNames = mutableListOf<String>()

        valueParametersAssociated.forEach { (bridge, p) ->
            var candidate = when (bridge) {
                is MethodBridgeValueParameter.Mapped -> {
                    p!!
                    when {
                        p is ReceiverParameterDescriptor -> "receiver"
                        method is PropertySetterDescriptor -> "value"
                        else -> p.name.asString()
                    }
                }
                MethodBridgeValueParameter.ErrorOutParameter -> "error"
                is MethodBridgeValueParameter.KotlinResultOutParameter -> "result"
            }
            while (candidate in valueParameterNames) {
                candidate += "_"
            }
            valueParameterNames += candidate
        }

        append(selectorParts[0])

        valueParametersAssociated.forEachIndexed { index, (bridge, p) ->
            val name = valueParameterNames[index]
            val type = when (bridge) {
                is MethodBridgeValueParameter.Mapped -> mapType(p!!.type, bridge.bridge)
                MethodBridgeValueParameter.ErrorOutParameter ->
                    ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

                is MethodBridgeValueParameter.KotlinResultOutParameter ->
                    ObjCPointerType(mapType(method.returnType!!, bridge.bridge), nullable = true)
            }

            if (index != 0) {
                append(' ')
                append(selectorParts[index])
            }

            append(":")
            append("(")
            append(type.render())
            append(")")
            append(name)
        }

        val swiftName = namer.getSwiftName(baseMethod)

        append(" NS_SWIFT_NAME($swiftName)")

        if (method is ConstructorDescriptor && !method.constructedClass.isArray) { // TODO: check methodBridge instead.
            append(" NS_DESIGNATED_INITIALIZER")
        }

        // TODO: consider adding swift_error attribute.
    }

    private val methodsWithThrowAnnotationConsidered = mutableSetOf<FunctionDescriptor>()

    private val uncheckedExceptionClasses = listOf("Error", "RuntimeException").map {
        context.builtIns.builtInsPackageScope
                .getContributedClassifier(Name.identifier(it), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    }

    private fun exportThrown(method: FunctionDescriptor) {
        if (!method.kind.isReal) return
        val throwsAnnotation = method.annotations.findAnnotation(KonanBuiltIns.FqNames.throws) ?: return

        if (!mapper.doesThrow(method)) {
            context.report(
                    context.ir.get(method),
                    "@${KonanBuiltIns.FqNames.throws.shortName()} annotation should also be added to a base method",
                    isError = false
            )
        }

        if (method in methodsWithThrowAnnotationConsidered) return
        methodsWithThrowAnnotationConsidered += method

        val arguments = (throwsAnnotation.allValueArguments.values.single() as ArrayValue).value
        for (argument in arguments) {
            val classDescriptor = TypeUtils.getClassDescriptor((argument as KClassValue).value) ?: continue

            uncheckedExceptionClasses.firstOrNull { classDescriptor.isSubclassOf(it) }?.let {
                context.report(
                        context.ir.get(method),
                        "Method is declared to throw ${classDescriptor.fqNameSafe}, " +
                                "but instances of ${it.fqNameSafe} and its subclasses aren't propagated " +
                                "from Kotlin to Objective-C/Swift",
                        isError = false
                )
            }

            scheduleClassToBeGenerated(classDescriptor)
        }
    }

    private fun exportThrownFromThisAndOverridden(method: FunctionDescriptor) {
        method.allOverriddenDescriptors.forEach { exportThrown(it) }
    }

    private fun mapReturnType(
            returnBridge: MethodBridge.ReturnValue,
            method: FunctionDescriptor
    ): ObjCType = when (returnBridge) {
        MethodBridge.ReturnValue.Void -> ObjCVoidType
        MethodBridge.ReturnValue.HashCode -> ObjCPrimitiveType("NSUInteger")
        is MethodBridge.ReturnValue.Mapped -> mapType(method.returnType!!, returnBridge.bridge)
        MethodBridge.ReturnValue.WithError.Success -> ObjCPrimitiveType("BOOL")
        is MethodBridge.ReturnValue.WithError.RefOrNull -> {
            val successReturnType = mapReturnType(returnBridge.successBridge, method)
            if (successReturnType !is ObjCNonNullReferenceType) {
                error("Function is expected to have non-null return type: $method")
            }

            ObjCNullableReferenceType(successReturnType)
        }

        MethodBridge.ReturnValue.Instance.InitResult,
        MethodBridge.ReturnValue.Instance.FactoryResult -> ObjCInstanceType
    }

    fun build(): List<String> = mutableListOf<String>().apply {
        add("#import <Foundation/Foundation.h>")
        add("")

        if (classForwardDeclarations.isNotEmpty()) {
            add("@class ${classForwardDeclarations.joinToString()};")
            add("")
        }

        if (protocolForwardDeclarations.isNotEmpty()) {
            add("@protocol ${protocolForwardDeclarations.joinToString()};")
            add("")
        }

        add("NS_ASSUME_NONNULL_BEGIN")
        add("")

        add("@interface $kotlinAnyName : NSObject")
        add("-(instancetype) init __attribute__((unavailable));")
        add("+(instancetype) new __attribute__((unavailable));")
        add("+(void)initialize __attribute__((objc_requires_super));")
        add("@end;")
        add("")

        // TODO: add comment to the header.
        add("@interface $kotlinAnyName (${kotlinAnyName}Copying) <NSCopying>")
        add("@end;")
        add("")

        add("__attribute__((objc_runtime_name(\"KotlinMutableSet\")))")
        add("@interface ${namer.mutableSetName}<ObjectType> : NSMutableSet<ObjectType>") // TODO: only if appears
        add("@end;")
        add("")

        add("__attribute__((objc_runtime_name(\"KotlinMutableDictionary\")))")
        add("@interface ${namer.mutableMapName}<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>") // TODO: only if appears
        add("@end;")
        add("")

        add("@interface NSError (NSErrorKotlinException)")
        add("@property (readonly) id _Nullable kotlinException;")
        add("@end;")
        add("")

        stubs.forEach {
            addAll(it.lines)
            add("")
        }

        add("NS_ASSUME_NONNULL_END")
    }
}

internal sealed class ObjCType {
    final override fun toString(): String = this.render()

    abstract fun render(attrsAndName: String): String

    fun render() = render("")

    protected fun String.withAttrsAndName(attrsAndName: String) =
            if (attrsAndName.isEmpty()) this else "$this ${attrsAndName.trimStart()}"
}

internal sealed class ObjCReferenceType : ObjCType()

internal sealed class ObjCNonNullReferenceType : ObjCReferenceType()

internal data class ObjCNullableReferenceType(val nonNullType: ObjCNonNullReferenceType) : ObjCReferenceType() {
    override fun render(attrsAndName: String) = nonNullType.render(" _Nullable".withAttrsAndName(attrsAndName))
}

private class ObjCClassType(
        val className: String,
        val typeArguments: List<ObjCNonNullReferenceType> = emptyList()
) : ObjCNonNullReferenceType() {

    override fun render(attrsAndName: String) = buildString {
        append(className)
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.joinTo(this) { it.render() }
            append(">")
        }
        append(" *")
        append(attrsAndName)
    }
}

private class ObjCProtocolType(val protocolName: String) : ObjCNonNullReferenceType() {

    override fun render(attrsAndName: String) = "id<$protocolName>".withAttrsAndName(attrsAndName)
}

private object ObjCIdType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = "id".withAttrsAndName(attrsAndName)
}

private object ObjCInstanceType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String = "instancetype".withAttrsAndName(attrsAndName)
}

private class ObjCBlockPointerType(
        val returnType: ObjCReferenceType, val parameterTypes: List<ObjCReferenceType>
) : ObjCNonNullReferenceType() {

    override fun render(attrsAndName: String) = returnType.render(buildString {
        append("(^")
        append(attrsAndName)
        append(")(")
        if (parameterTypes.isEmpty()) append("void")
        parameterTypes.joinTo(this) { it.render() }
        append(')')
    })
}

private class ObjCPrimitiveType(val cName: String) : ObjCType() {
    override fun render(attrsAndName: String) = cName.withAttrsAndName(attrsAndName)
}

private class ObjCPointerType(val pointee: ObjCType, val nullable: Boolean = false) : ObjCType() {
    override fun render(attrsAndName: String) =
            pointee.render("*${if (nullable) {
                " _Nullable".withAttrsAndName(attrsAndName)
            } else {
                attrsAndName
            }}")
}

private object ObjCVoidType : ObjCType() {
    override fun render(attrsAndName: String) = "void".withAttrsAndName(attrsAndName)
}

internal interface CustomTypeMapper {
    val mappedClassDescriptor: ClassDescriptor
    fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType

    class Simple(
            override val mappedClassDescriptor: ClassDescriptor,
            private val objCClassName: String
    ) : CustomTypeMapper {

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType =
                ObjCClassType(objCClassName)
    }

    class Collection(
            private val generator: ObjCExportHeaderGenerator,
            override val mappedClassDescriptor: ClassDescriptor,
            private val objCClassName: String
    ) : CustomTypeMapper {
        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType {
            val typeArguments = mappedSuperType.arguments.map {
                val argument = it.type
                if (TypeUtils.isNullableType(argument)) {
                    // Kotlin `null` keys and values are represented as `NSNull` singleton.
                    ObjCIdType
                } else {
                    generator.mapReferenceTypeIgnoringNullability(argument)
                }
            }

            return ObjCClassType(objCClassName, typeArguments)
        }
    }

    class Function(
            private val generator: ObjCExportHeaderGenerator,
            parameterCount: Int
    ) : CustomTypeMapper {
        override val mappedClassDescriptor = generator.context.builtIns.getFunction(parameterCount)

        override fun mapType(mappedSuperType: KotlinType): ObjCNonNullReferenceType {
            val functionType = mappedSuperType

            val returnType = functionType.getReturnTypeFromFunctionType()
            val parameterTypes = listOfNotNull(functionType.getReceiverTypeFromFunctionType()) +
                    functionType.getValueParameterTypesFromFunctionType().map { it.type }

            return ObjCBlockPointerType(
                    generator.mapReferenceType(returnType),
                    parameterTypes.map { generator.mapReferenceType(it) }
            )
        }
    }
}

private fun ObjCExportHeaderGenerator.mapReferenceType(kotlinType: KotlinType): ObjCReferenceType =
        mapReferenceTypeIgnoringNullability(kotlinType).let {
            if (TypeUtils.isNullableType(kotlinType)) {
                ObjCNullableReferenceType(it)
            } else {
                it
            }
        }

private fun ObjCExportHeaderGenerator.mapReferenceTypeIgnoringNullability(
        kotlinType: KotlinType
): ObjCNonNullReferenceType {
    val typeToMapper = (listOf(kotlinType) + kotlinType.supertypes()).mapNotNull { type ->
        val mapper = customTypeMappers[type.constructor.declarationDescriptor]
        if (mapper != null) {
            type to mapper
        } else {
            null
        }
    }.toMap()

    val mostSpecificTypeToMapper = typeToMapper.filter { (_, mapper) ->
        typeToMapper.values.all { it.mappedClassDescriptor == mapper.mappedClassDescriptor ||
                !it.mappedClassDescriptor.isSubclassOf(mapper.mappedClassDescriptor) }

        // E.g. if both List and MutableList are present, then retain only MutableList.
    }

    if (mostSpecificTypeToMapper.size > 1) {
        val types = mostSpecificTypeToMapper.keys.toList()
        val firstType = types[0]
        val secondType = types[1]

        context.reportCompilationWarning(
                "Exposed type '$kotlinType' is '$firstType' and '$secondType' at the same time. " +
                        "This most likely wouldn't work as expected.")

        // TODO: the same warning for such classes.
    }

    mostSpecificTypeToMapper.entries.firstOrNull()?.let { (type, mapper) ->
        return mapper.mapType(type)
    }

    val classDescriptor = kotlinType.getErasedTypeClass()

    // TODO: translate `where T : BaseClass, T : SomeInterface` to `BaseClass* <SomeInterface>`

    if (classDescriptor == context.builtIns.any || classDescriptor in hiddenTypes) {
        return ObjCIdType
    }

    if (classDescriptor.defaultType.isObjCObjectType()) {
        return mapObjCObjectReferenceTypeIgnoringNullability(classDescriptor)
    }

    scheduleClassToBeGenerated(classDescriptor)

    return if (classDescriptor.isInterface) {
        ObjCProtocolType(translateClassName(classDescriptor))
    } else {
        ObjCClassType(translateClassName(classDescriptor))
    }
}

private tailrec fun ObjCExportHeaderGenerator.mapObjCObjectReferenceTypeIgnoringNullability(
        descriptor: ClassDescriptor
): ObjCNonNullReferenceType {
    // TODO: more precise types can be used.

    if (descriptor.isObjCMetaClass()) return ObjCIdType

    if (descriptor.isExternalObjCClass()) {
        return if (descriptor.isInterface) {
            val name = descriptor.name.asString().removeSuffix("Protocol")
            protocolForwardDeclarations += name
            ObjCProtocolType(name)
        } else {
            val name = descriptor.name.asString()
            classForwardDeclarations += name
            ObjCClassType(name)
        }
    }

    if (descriptor.isKotlinObjCClass()) {
        return mapObjCObjectReferenceTypeIgnoringNullability(descriptor.getSuperClassOrAny())
    }

    return ObjCIdType
}

private fun ObjCExportHeaderGenerator.scheduleClassToBeGenerated(classDescriptor: ClassDescriptor) {
    if (classDescriptor !in generatedClasses) {
        extraClassesToTranslate += classDescriptor
    }
}

private fun ObjCExportHeaderGenerator.mapType(
        kotlinType: KotlinType,
        typeBridge: TypeBridge
): ObjCType = when (typeBridge) {
    ReferenceBridge -> mapReferenceType(kotlinType)
    is ValueTypeBridge -> {
        val cName = when (typeBridge.objCValueType) {
            ObjCValueType.BOOL -> "BOOL"
            ObjCValueType.CHAR -> "int8_t"
            ObjCValueType.UNSIGNED_SHORT -> "unichar"
            ObjCValueType.SHORT -> "int16_t"
            ObjCValueType.INT -> "int32_t"
            ObjCValueType.LONG_LONG -> "int64_t"
            ObjCValueType.FLOAT -> "float"
            ObjCValueType.DOUBLE -> "double"
        }
        // TODO: consider other namings.
        ObjCPrimitiveType(cName)
    }
}

private data class Stub(val lines: List<String>)

private class StubBuilder {
    private val lines = mutableListOf<String>()

    operator fun String.unaryPlus() {
        lines.add(this)
    }

    operator fun Stub.unaryPlus() {
        this@StubBuilder.lines.addAll(this.lines)
    }

    fun build() = Stub(lines)
}

private inline fun buildStub(block: StubBuilder.() -> Unit) = StubBuilder().let {
    it.block()
    it.build()
}

private inline fun MutableCollection<Stub>.addBuiltBy(block: StubBuilder.() -> Unit) {
    this.add(buildStub(block))
}
