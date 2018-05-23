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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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

abstract class ObjCExportHeaderGenerator(
        val moduleDescriptor: ModuleDescriptor,
        val builtIns: KotlinBuiltIns,
        topLevelNamePrefix: String = moduleDescriptor.namePrefix
) {
    internal val mapper: ObjCExportMapper = object : ObjCExportMapper() {
        override fun getCategoryMembersFor(descriptor: ClassDescriptor) =
                extensions[descriptor].orEmpty()

        override fun isSpecialMapped(descriptor: ClassDescriptor): Boolean {
            // TODO: this method duplicates some of the [mapReferenceType] logic.
            return descriptor == builtIns.any ||
                   descriptor.getAllSuperClassifiers().any { it in customTypeMappers }
        }
    }

    internal val namer = ObjCExportNamer(moduleDescriptor, builtIns, mapper, topLevelNamePrefix)

    internal val generatedClasses = mutableSetOf<ClassDescriptor>()
    internal val topLevel = mutableMapOf<FqName, MutableList<CallableMemberDescriptor>>()

    private val customTypeMappers: Map<ClassDescriptor, CustomTypeMapper> = with(builtIns) {
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

        (0..mapper.maxFunctionTypeParameterCount).forEach {
            result += CustomTypeMapper.Function(generator, it)
        }

        result.associateBy { it.mappedClassDescriptor }
    }

    private val hiddenTypes: Set<ClassDescriptor> = run {
        val customMappedTypes = customTypeMappers.keys

        customMappedTypes
                .asSequence()
                .flatMap { it.getAllSuperClassifiers().asSequence() }
                .map { it as ClassDescriptor }
                .filter { !customMappedTypes.contains(it) }
                .toSet()
    }

    private val kotlinAnyName = namer.kotlinAnyName

    private val stubs = mutableListOf<Stub<*>>()
    private val classOrInterfaceToName = mutableMapOf<ClassDescriptor, String>()

    private val classForwardDeclarations = mutableSetOf<String>()
    private val protocolForwardDeclarations = mutableSetOf<String>()

    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    fun translateModule(): List<Stub<*>> {
        // TODO: make the translation order stable
        // to stabilize name mangling.

        stubs.add(ObjCInterface(kotlinAnyName, superClass = "NSObject", members = buildMembers {
            +ObjCMethod(null, true, ObjCInstanceType, listOf("init"), emptyList(), listOf("unavailable"))
            +ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable"))
            +ObjCMethod(null, false, ObjCVoidType, listOf("initialize"), emptyList(), listOf("objc_requires_super"))
        }))

        // TODO: add comment to the header.
        stubs.add(ObjCInterface(
                kotlinAnyName,
                superProtocols = listOf("NSCopying"),
                categoryName = "${kotlinAnyName}Copying"
        ))

        // TODO: only if appears
        stubs.add(ObjCInterface(
                namer.mutableSetName,
                generics = listOf("ObjectType"),
                superClass = "NSMutableSet<ObjectType>",
                attributes = listOf("objc_runtime_name(\"KotlinMutableSet\")")
        ))

        // TODO: only if appears
        stubs.add(ObjCInterface(
                namer.mutableMapName,
                generics = listOf("KeyType", "ObjectType"),
                superClass = "NSMutableDictionary<KeyType, ObjectType>",
                attributes = listOf("objc_runtime_name(\"KotlinMutableDictionary\")")
        ))

        stubs.add(ObjCInterface("NSError", categoryName = "NSErrorKotlinException", members = buildMembers {
            +ObjCProperty("kotlinException", null, ObjCNullableReferenceType(ObjCIdType), listOf("readonly"))
        }))

        val packageFragments = moduleDescriptor.getPackageFragments()

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .asSequence()
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

        fun MemberScope.translateClasses() {
            getContributedDescriptors()
                    .asSequence()
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

        return stubs
    }

    private fun translateClassName(descriptor: ClassDescriptor): String = classOrInterfaceToName.getOrPut(descriptor) {
        assert(mapper.shouldBeExposed(descriptor))
        val forwardDeclarations = if (descriptor.isInterface) protocolForwardDeclarations else classForwardDeclarations

        namer.getClassOrProtocolName(descriptor).also { forwardDeclarations += it }
    }

    private fun translateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return

        val name: String = translateClassName(descriptor)
        val members: List<Stub<*>> = buildMembers { translateClassOrInterfaceMembers(descriptor) }
        val superProtocols: List<String> = descriptor.superProtocols

        val protocolStub = ObjCProtocol(name, descriptor, superProtocols, members)

        stubs.add(protocolStub)
    }

    private val ClassDescriptor.superProtocols: List<String>
        get() =
            getSuperInterfaces()
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .map {
                        translateInterface(it)
                        translateClassName(it)
                    }
                    .toList()

    private fun translateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        translateClass(classDescriptor)

        val name = translateClassName(classDescriptor)
        val members = buildMembers {
            translateMembers(declarations)
        }
        stubs.add(ObjCInterface(name, categoryName = "Extensions", members = members))
    }

    private fun translateTopLevel(packageFqName: FqName, declarations: List<CallableMemberDescriptor>) {
        val name = namer.getPackageName(packageFqName)

        // TODO: stop inheriting KotlinBase.
        val members = buildMembers {
            translateMembers(declarations)
        }
        stubs.add(ObjCInterface(
                name,
                superClass = namer.kotlinAnyName,
                members = members,
                attributes = listOf("objc_subclassing_restricted")
        ))
    }

    private fun translateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return

        val name = translateClassName(descriptor)
        val superClass = descriptor.getSuperClassNotAny()

        val superName: String = if (superClass == null) {
            kotlinAnyName
        } else {
            translateClass(superClass)
            translateClassName(superClass)
        }

        val superProtocols: List<String> = descriptor.superProtocols
        val members: List<Stub<*>> = buildMembers {
            val presentConstructors = mutableSetOf<String>()

            descriptor.constructors
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val selector = getSelector(it)
                        if (!descriptor.isArray) presentConstructors += selector

                        +buildMethod(it, it)
                        if (selector == "init") {
                            +ObjCMethod(it, false, ObjCInstanceType, listOf("new"), emptyList(),
                                        listOf("availability(swift, unavailable, message=\"use object initializers instead\")"))
                        }
                    }

            if (descriptor.isArray || descriptor.kind == ClassKind.OBJECT || descriptor.kind == ClassKind.ENUM_CLASS) {
                +ObjCMethod(null, false, ObjCInstanceType, listOf("alloc"), emptyList(), listOf("unavailable"))

                val parameter = ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"))
                +ObjCMethod(descriptor, false, ObjCInstanceType, listOf("allocWithZone:"), listOf(parameter), listOf("unavailable"))
            }

            // TODO: consider adding exception-throwing impls for these.
            when (descriptor.kind) {
                ClassKind.OBJECT -> {
                    +ObjCMethod(
                            null, false, ObjCInstanceType,
                            listOf(namer.getObjectInstanceSelector(descriptor)), emptyList(),
                            listOf(swiftNameAttribute("init()"))
                    )
                }
                ClassKind.ENUM_CLASS -> {
                    val type = mapType(descriptor.defaultType, ReferenceBridge)

                    descriptor.enumEntries.forEach {
                        val entryName = namer.getEnumEntrySelector(it)
                        +ObjCProperty(entryName, null, type, listOf("class", "readonly"))
                    }
                }
                else -> {
                    // Nothing special.
                }
            }

            // Hide "unimplemented" super constructors:
            superClass?.constructors
                    ?.asSequence()
                    ?.filter { mapper.shouldBeExposed(it) }
                    ?.forEach {
                        val selector = getSelector(it)
                        if (selector !in presentConstructors) {
                            val c = buildMethod(it, it)
                            +ObjCMethod(c.descriptor, c.isInstanceMethod, c.returnType, c.selectors, c.parameters, c.attributes + "unavailable")

                            if (selector == "init") {
                                +ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable"))
                            }

                            // TODO: consider adding exception-throwing impls for these.
                        }
                    }

            translateClassOrInterfaceMembers(descriptor)
        }

        val attributes = if (descriptor.isFinalOrEnum) listOf("objc_subclassing_restricted") else emptyList()

        val interfaceStub = ObjCInterface(
                name,
                descriptor = descriptor,
                superClass = superName,
                superProtocols = superProtocols,
                members = members,
                attributes = attributes
        )
        stubs.add(interfaceStub)
    }

    private fun StubBuilder.translateClassOrInterfaceMembers(descriptor: ClassDescriptor) {
        val members = descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                .asSequence()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { mapper.shouldBeExposed(it) }
                .toList()

        translateMembers(members)
    }

    private class RenderedStub<T: Stub<*>>(val stub: T) {
        private val presentation: String by lazy(LazyThreadSafetyMode.NONE) {
            val listOfLines = StubRenderer.render(stub)
            assert(listOfLines.size == 1)
            listOfLines[0]
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return other is RenderedStub<*> && presentation == other.presentation
        }

        override fun hashCode(): Int {
            return presentation.hashCode()
        }
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

        collectMethodsOrProperties(methods) { it -> buildMethods(it.original) }
        collectMethodsOrProperties(properties) { it -> buildProperties(it.original) }
    }

    private fun <D : CallableMemberDescriptor, S : Stub<*>> StubBuilder.collectMethodsOrProperties(
            members: List<D>,
            converter: (D) -> Set<RenderedStub<S>>) {
        members.forEach { member ->
            val superMembers: Set<RenderedStub<S>> = (member.overriddenDescriptors as Collection<D>)
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .flatMap { converter(it).asSequence() }
                    .toSet()

            this += converter(member)
                    .asSequence()
                    .filterNot { superMembers.contains(it) }
                    .map { rendered -> rendered.stub }
                    .toList()
        }
    }

    private val methodToSignatures = mutableMapOf<FunctionDescriptor, Set<RenderedStub<ObjCMethod>>>()
    private val propertyToSignatures = mutableMapOf<PropertyDescriptor, Set<RenderedStub<ObjCProperty>>>()

    private fun buildMethods(method: FunctionDescriptor): Set<RenderedStub<ObjCMethod>> = methodToSignatures.getOrPut(method) {
        mapper.getBaseMethods(method)
                .asSequence()
                .distinctBy { namer.getSelector(it) }
                .map { base -> buildMethod(method, base) }
                .map { method -> RenderedStub(method) }
                .toSet()
    }

    private fun buildProperties(property: PropertyDescriptor): Set<RenderedStub<ObjCProperty>> = propertyToSignatures.getOrPut(property) {
        mapper.getBaseProperties(property)
                .asSequence()
                .distinctBy { namer.getName(it) }
                .map { base -> buildProperty(property, base) }
                .map { property -> RenderedStub(property) }
                .toSet()
    }

    // TODO: consider checking that signatures for bases with same selector/name are equal.

    private fun getSelector(method: FunctionDescriptor): String {
        return namer.getSelector(method)
    }

    private fun buildProperty(property: PropertyDescriptor, baseProperty: PropertyDescriptor): ObjCProperty {
        assert(mapper.isBaseProperty(baseProperty))
        assert(mapper.isObjCProperty(baseProperty))

        val getterBridge = mapper.bridgeMethod(baseProperty.getter!!)
        val type = mapReturnType(getterBridge.returnBridge, property.getter!!)
        val name = namer.getName(baseProperty)

        val attributes = mutableListOf<String>()

        if (!getterBridge.isInstance) {
            attributes += "class"
        }

        val setterName: String?
        val propertySetter = property.setter
        if (propertySetter != null && mapper.shouldBeExposed(propertySetter)) {
            val setterSelector = mapper.getBaseMethods(propertySetter).map { namer.getSelector(it) }.distinct().single()
            setterName = if (setterSelector != "set" + name.capitalize() + ":") setterSelector else null
        } else {
            attributes += "readonly"
            setterName = null
        }

        val getterSelector = getSelector(baseProperty.getter!!)
        val getterName: String? = if (getterSelector != name) getterSelector else null

        return ObjCProperty(name, property, type, attributes, setterName, getterName)
    }

    private fun buildMethod(method: FunctionDescriptor, baseMethod: FunctionDescriptor): ObjCMethod {
        fun collectParameters(baseMethodBridge: MethodBridge, method: FunctionDescriptor): List<ObjCParameter> {
            fun unifyName(initialName: String, usedNames: Set<String>): String {
                var unique = initialName
                while (unique in usedNames) {
                    unique += "_"
                }
                return unique
            }

            val valueParametersAssociated = baseMethodBridge.valueParametersAssociated(method)

            val parameters = mutableListOf<ObjCParameter>()

            val usedNames = mutableSetOf<String>()
            valueParametersAssociated.forEach { (bridge: MethodBridgeValueParameter, p: ParameterDescriptor?) ->
                val candidateName: String = when (bridge) {
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

                val uniqueName = unifyName(candidateName, usedNames)
                usedNames += uniqueName

                val type = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> mapType(p!!.type, bridge.bridge)
                    MethodBridgeValueParameter.ErrorOutParameter ->
                        ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

                    is MethodBridgeValueParameter.KotlinResultOutParameter ->
                        ObjCPointerType(mapType(method.returnType!!, bridge.bridge), nullable = true)
                }

                parameters += ObjCParameter(uniqueName, p, type)
            }
            return parameters
        }

        assert(mapper.isBaseMethod(baseMethod))

        val baseMethodBridge = mapper.bridgeMethod(baseMethod)

        exportThrownFromThisAndOverridden(method)

        val isInstanceMethod: Boolean = baseMethodBridge.isInstance
        val returnType: ObjCType = mapReturnType(baseMethodBridge.returnBridge, method)
        val parameters = collectParameters(baseMethodBridge, method)
        val selector = getSelector(baseMethod)
        val selectorParts: List<String> = splitSelector(selector)
        val swiftName = namer.getSwiftName(baseMethod)
        val attributes = mutableListOf<String>()

        attributes += swiftNameAttribute(swiftName)

        if (method is ConstructorDescriptor && !method.constructedClass.isArray) { // TODO: check methodBridge instead.
            attributes += "objc_designated_initializer"
        }

        return ObjCMethod(method, isInstanceMethod, returnType, selectorParts, parameters, attributes)
    }

    private fun splitSelector(selector: String): List<String> {
        return if (!selector.endsWith(":")) {
            listOf(selector)
        } else {
            selector.trimEnd(':').split(':').map { "$it:" }
        }
    }

    private fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"

    private val methodsWithThrowAnnotationConsidered = mutableSetOf<FunctionDescriptor>()

    private val uncheckedExceptionClasses = listOf("Error", "RuntimeException").map {
        builtIns.builtInsPackageScope
                .getContributedClassifier(Name.identifier(it), NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    }

    private fun exportThrown(method: FunctionDescriptor) {
        if (!method.kind.isReal) return
        val throwsAnnotation = method.annotations.findAnnotation(KonanBuiltIns.FqNames.throws) ?: return

        if (!mapper.doesThrow(method)) {
            reportWarning(method, "@${KonanBuiltIns.FqNames.throws.shortName()} annotation should also be added to a base method")
        }

        if (method in methodsWithThrowAnnotationConsidered) return
        methodsWithThrowAnnotationConsidered += method

        val arguments = (throwsAnnotation.allValueArguments.values.single() as ArrayValue).value
        for (argument in arguments) {
            val classDescriptor = TypeUtils.getClassDescriptor((argument as KClassValue).value) ?: continue

            uncheckedExceptionClasses.firstOrNull { classDescriptor.isSubclassOf(it) }?.let {
                reportWarning(method,
                        "Method is declared to throw ${classDescriptor.fqNameSafe}, " +
                                "but instances of ${it.fqNameSafe} and its subclasses aren't propagated " +
                                "from Kotlin to Objective-C/Swift")
            }

            scheduleClassToBeGenerated(classDescriptor)
        }
    }

    private fun exportThrownFromThisAndOverridden(method: FunctionDescriptor) {
        method.allOverriddenDescriptors.forEach { exportThrown(it) }
    }

    private fun mapReturnType(returnBridge: MethodBridge.ReturnValue, method: FunctionDescriptor): ObjCType = when (returnBridge) {
        MethodBridge.ReturnValue.Void -> ObjCVoidType
        MethodBridge.ReturnValue.HashCode -> ObjCPrimitiveType("NSUInteger")
        is MethodBridge.ReturnValue.Mapped -> mapType(method.returnType!!, returnBridge.bridge)
        MethodBridge.ReturnValue.WithError.Success -> ObjCPrimitiveType("BOOL")
        is MethodBridge.ReturnValue.WithError.RefOrNull -> {
            val successReturnType = mapReturnType(returnBridge.successBridge, method) as? ObjCNonNullReferenceType
                    ?: error("Function is expected to have non-null return type: $method")

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

        stubs.forEach {
            addAll(StubRenderer.render(it))
            add("")
        }

        add("NS_ASSUME_NONNULL_END")
    }

    protected abstract fun reportWarning(text: String)

    protected abstract fun reportWarning(method: FunctionDescriptor, text: String)

    internal fun mapReferenceType(kotlinType: KotlinType): ObjCReferenceType =
            mapReferenceTypeIgnoringNullability(kotlinType).let {
                if (TypeUtils.isNullableType(kotlinType)) {
                    ObjCNullableReferenceType(it)
                } else {
                    it
                }
            }

    internal fun mapReferenceTypeIgnoringNullability(kotlinType: KotlinType): ObjCNonNullReferenceType {
        val typeToMapper = (listOf(kotlinType) + kotlinType.supertypes()).mapNotNull { type ->
            val mapper = customTypeMappers[type.constructor.declarationDescriptor]
            if (mapper != null) {
                type to mapper
            } else {
                null
            }
        }.toMap()

        val mostSpecificTypeToMapper = typeToMapper.filter { (_, mapper) ->
            typeToMapper.values.all {
                it.mappedClassDescriptor == mapper.mappedClassDescriptor ||
                !it.mappedClassDescriptor.isSubclassOf(mapper.mappedClassDescriptor)
            }

            // E.g. if both List and MutableList are present, then retain only MutableList.
        }

        if (mostSpecificTypeToMapper.size > 1) {
            val types = mostSpecificTypeToMapper.keys.toList()
            val firstType = types[0]
            val secondType = types[1]

            reportWarning("Exposed type '$kotlinType' is '$firstType' and '$secondType' at the same time. " +
                                     "This most likely wouldn't work as expected.")

            // TODO: the same warning for such classes.
        }

        mostSpecificTypeToMapper.entries.firstOrNull()?.let { (type, mapper) ->
            return mapper.mapType(type)
        }

        val classDescriptor = kotlinType.getErasedTypeClass()

        // TODO: translate `where T : BaseClass, T : SomeInterface` to `BaseClass* <SomeInterface>`

        if (classDescriptor == builtIns.any || classDescriptor in hiddenTypes) {
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

    private tailrec fun mapObjCObjectReferenceTypeIgnoringNullability(descriptor: ClassDescriptor): ObjCNonNullReferenceType {
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

    private fun scheduleClassToBeGenerated(classDescriptor: ClassDescriptor) {
        if (classDescriptor !in generatedClasses) {
            extraClassesToTranslate += classDescriptor
        }
    }

    private fun mapType(kotlinType: KotlinType, typeBridge: TypeBridge): ObjCType = when (typeBridge) {
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
}