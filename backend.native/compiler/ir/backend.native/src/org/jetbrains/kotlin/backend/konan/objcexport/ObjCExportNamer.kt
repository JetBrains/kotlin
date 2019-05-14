/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.cKeywords
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isKonanStdlib
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.source.PsiSourceFile

interface ObjCExportNamer {
    data class ClassOrProtocolName(val swiftName: String, val objCName: String, val binaryName: String = objCName)

    fun getFileClassName(file: SourceFile): ClassOrProtocolName
    fun getClassOrProtocolName(descriptor: ClassDescriptor): ClassOrProtocolName
    fun getSelector(method: FunctionDescriptor): String
    fun getSwiftName(method: FunctionDescriptor): String
    fun getPropertyName(property: PropertyDescriptor): String
    fun getObjectInstanceSelector(descriptor: ClassDescriptor): String
    fun getEnumEntrySelector(descriptor: ClassDescriptor): String
    fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String

    fun numberBoxName(classId: ClassId): ClassOrProtocolName

    val kotlinAnyName: ClassOrProtocolName
    val mutableSetName: ClassOrProtocolName
    val mutableMapName: ClassOrProtocolName
    val kotlinNumberName: ClassOrProtocolName
}

fun createNamer(moduleDescriptor: ModuleDescriptor,
                topLevelNamePrefix: String = moduleDescriptor.namePrefix): ObjCExportNamer =
        createNamer(moduleDescriptor, emptyList(), topLevelNamePrefix)

fun createNamer(
        moduleDescriptor: ModuleDescriptor,
        exportedDependencies: List<ModuleDescriptor>,
        topLevelNamePrefix: String = moduleDescriptor.namePrefix
): ObjCExportNamer = ObjCExportNamerImpl(
        (exportedDependencies + moduleDescriptor).toSet(),
        moduleDescriptor.builtIns,
        ObjCExportMapper(),
        topLevelNamePrefix,
        local = true
)

internal class ObjCExportNamerImpl(
        private val moduleDescriptors: Set<ModuleDescriptor>,
        builtIns: KotlinBuiltIns,
        private val mapper: ObjCExportMapper,
        private val topLevelNamePrefix: String,
        private val local: Boolean,
        private val objcGenerics: Boolean = false
) : ObjCExportNamer {
    private fun String.toUnmangledClassOrProtocolName(): ObjCExportNamer.ClassOrProtocolName =
            ObjCExportNamer.ClassOrProtocolName(swiftName = this, objCName = this)

    private fun String.toSpecialStandardClassOrProtocolName() = ObjCExportNamer.ClassOrProtocolName(
            swiftName = "Kotlin$this",
            objCName = "${topLevelNamePrefix}$this",
            binaryName = "Kotlin$this"
    )

    override val kotlinAnyName = "KotlinBase".toUnmangledClassOrProtocolName()

    override val mutableSetName = "MutableSet".toSpecialStandardClassOrProtocolName()
    override val mutableMapName = "MutableDictionary".toSpecialStandardClassOrProtocolName()

    override fun numberBoxName(classId: ClassId): ObjCExportNamer.ClassOrProtocolName =
            classId.shortClassName.asString().toSpecialStandardClassOrProtocolName()

    override val kotlinNumberName = "Number".toSpecialStandardClassOrProtocolName()

    private val methodSelectors = object : Mapping<FunctionDescriptor, String>() {

        // Try to avoid clashing with critical NSObject instance methods:

        private val reserved = setOf(
                "retain", "release", "autorelease",
                "class", "superclass",
                "hash"
        )

        override fun reserved(name: String) = name in reserved

        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean =
                !mapper.canHaveSameSelector(first, second)
    }

    private val methodSwiftNames = object : Mapping<FunctionDescriptor, String>() {
        override fun conflict(first: FunctionDescriptor, second: FunctionDescriptor): Boolean =
                !mapper.canHaveSameSelector(first, second)
        // Note: this condition is correct but can be too strict.
    }

    private val propertyNames = object : Mapping<PropertyDescriptor, String>() {
        override fun reserved(name: String) = name in cKeywords

        override fun conflict(first: PropertyDescriptor, second: PropertyDescriptor): Boolean =
                !mapper.canHaveSameName(first, second)
    }

    private open inner class GlobalNameMapping<in T : Any, N> : Mapping<T, N>() {
        final override fun conflict(first: T, second: T): Boolean = true
    }

    private val objCClassNames = GlobalNameMapping<Any, String>()
    private val objCProtocolNames = GlobalNameMapping<ClassDescriptor, String>()

    // Classes and protocols share the same namespace in Swift.
    private val swiftClassAndProtocolNames = GlobalNameMapping<Any, String>()

    private val genericTypeParameterNameMapping = GenericTypeParameterNameMapping()

    private abstract inner class ClassPropertyNameMapping<T : Any> : Mapping<T, String>() {

        // Try to avoid clashing with NSObject class methods:

        private val reserved = setOf(
                "retain", "release", "autorelease",
                "initialize", "load", "alloc", "new", "class", "superclass",
                "classFallbacksForKeyedArchiver", "classForKeyedUnarchiver",
                "description", "debugDescription", "version", "hash",
                "useStoredAccessor"
        )

        override fun reserved(name: String) = (name in reserved) || (name in cKeywords)
    }

    private val objectInstanceSelectors = object : ClassPropertyNameMapping<ClassDescriptor>() {
        override fun conflict(first: ClassDescriptor, second: ClassDescriptor) = false
    }

    private val enumEntrySelectors = object : ClassPropertyNameMapping<ClassDescriptor>() {
        override fun conflict(first: ClassDescriptor, second: ClassDescriptor) =
                first.containingDeclaration == second.containingDeclaration
    }

    override fun getFileClassName(file: SourceFile): ObjCExportNamer.ClassOrProtocolName {
        val baseName by lazy {
            val fileName = when (file) {
                is PsiSourceFile -> {
                    val psiFile = file.psiFile
                    val ktFile = psiFile as? KtFile ?: error("PsiFile '$psiFile' is not KtFile")
                    ktFile.name
                }
                else -> file.name ?: error("$file has no name")
            }
            PackagePartClassUtils.getFilePartShortName(fileName)
        }

        val objCName = objCClassNames.getOrPut(file) {
            StringBuilder(topLevelNamePrefix).append(baseName)
                    .mangledBySuffixUnderscores()
        }

        val swiftName = swiftClassAndProtocolNames.getOrPut(file) {
            StringBuilder(baseName)
                    .mangledBySuffixUnderscores()
        }

        return ObjCExportNamer.ClassOrProtocolName(swiftName = swiftName, objCName = objCName)
    }

    private val predefinedClassNames = mapOf(
            builtIns.any to kotlinAnyName,
            builtIns.mutableSet to mutableSetName,
            builtIns.mutableMap to mutableMapName
    )

    override fun getClassOrProtocolName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName =
            predefinedClassNames[descriptor]
                    ?: ObjCExportNamer.ClassOrProtocolName(
                            swiftName = getClassOrProtocolSwiftName(descriptor),
                            objCName = getClassOrProtocolObjCName(descriptor)
                    )

    private fun getClassOrProtocolSwiftName(
            descriptor: ClassDescriptor
    ): String = swiftClassAndProtocolNames.getOrPut(descriptor) {
        StringBuilder().apply {
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration is ClassDescriptor) {
                append(getClassOrProtocolSwiftName(containingDeclaration))

                val importAsMember = when {
                    objcGenerics && descriptor.hasGenericsInHierarchy() -> false

                    descriptor.isInterface || containingDeclaration.isInterface -> {
                        // Swift doesn't support neither nested nor outer protocols.
                        false
                    }

                    this.contains('.') -> {
                        // Swift doesn't support swift_name with deeply nested names.
                        // It seems to support "OriginalObjCName.SwiftName" though,
                        // but this doesn't seem neither documented nor reliable.
                        false
                    }

                    else -> true
                }
                if (importAsMember) {
                    append(".").append(descriptor.name.asString())
                } else {
                    append(descriptor.name.asString().capitalize())
                }
            } else if (containingDeclaration is PackageFragmentDescriptor) {
                appendTopLevelClassBaseName(descriptor)
            } else {
                error("unexpected class parent: $containingDeclaration")
            }
        }.mangledBySuffixUnderscores()
    }

    private fun ClassDescriptor.hasGenericsInHierarchy(): Boolean {
        fun ClassDescriptor.hasGenericsChildren(): Boolean =
                unsubstitutedMemberScope.getContributedDescriptors()
                        .asSequence()
                        .filterIsInstance<ClassDescriptor>()
                        .any {
                            it.typeConstructor.parameters.isNotEmpty() ||
                                    it.hasGenericsChildren()
                        }

        val upGenerics = generateSequence(this) { it.containingDeclaration as? ClassDescriptor }
                .any { it.typeConstructor.parameters.isNotEmpty() }

        return upGenerics || hasGenericsChildren()
    }

    private fun getClassOrProtocolObjCName(descriptor: ClassDescriptor): String {
        val objCMapping = if (descriptor.isInterface) objCProtocolNames else objCClassNames
        return objCMapping.getOrPut(descriptor) {
            StringBuilder().apply {
                val containingDeclaration = descriptor.containingDeclaration
                if (containingDeclaration is ClassDescriptor) {
                    append(getClassOrProtocolObjCName(containingDeclaration))
                            .append(descriptor.name.asString().capitalize())

                } else if (containingDeclaration is PackageFragmentDescriptor) {
                    append(topLevelNamePrefix).appendTopLevelClassBaseName(descriptor)
                } else {
                    error("unexpected class parent: $containingDeclaration")
                }
            }.mangledBySuffixUnderscores()
        }
    }

    private fun StringBuilder.appendTopLevelClassBaseName(descriptor: ClassDescriptor) = apply {
        if (descriptor.module !in moduleDescriptors) {
            append(descriptor.module.namePrefix)
        }
        append(descriptor.name.asString())
    }

    override fun getSelector(method: FunctionDescriptor): String = methodSelectors.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        val parameters = mapper.bridgeMethod(method).valueParametersAssociated(method)

        StringBuilder().apply {
            append(method.getMangledName(forSwift = false))

            parameters.forEachIndexed { index, (bridge, it) ->
                val name = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> when {
                        it is ReceiverParameterDescriptor -> ""
                        method is PropertySetterDescriptor -> when (parameters.size) {
                            1 -> ""
                            else -> "value"
                        }
                        else -> it!!.name.asString()
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> "error"
                    is MethodBridgeValueParameter.KotlinResultOutParameter -> "result"
                }

                if (index == 0) {
                    append(when {
                        bridge is MethodBridgeValueParameter.ErrorOutParameter ||
                                bridge is MethodBridgeValueParameter.KotlinResultOutParameter -> "AndReturn"

                        method is ConstructorDescriptor -> "With"
                        else -> ""
                    })
                    append(name.capitalize())
                } else {
                    append(name)
                }

                append(':')
            }
        }.mangledSequence {
            if (parameters.isNotEmpty()) {
                // "foo:" -> "foo_:"
                insert(lastIndex, '_')
            } else {
                // "foo" -> "foo_"
                append("_")
            }
        }
    }

    override fun getSwiftName(method: FunctionDescriptor): String = methodSwiftNames.getOrPut(method) {
        assert(mapper.isBaseMethod(method))

        val parameters = mapper.bridgeMethod(method).valueParametersAssociated(method)

        StringBuilder().apply {
            append(method.getMangledName(forSwift = true))
            append("(")

            parameters@ for ((bridge, it) in parameters) {
                val label = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> when {
                        it is ReceiverParameterDescriptor -> "_"
                        method is PropertySetterDescriptor -> when (parameters.size) {
                            1 -> "_"
                            else -> "value"
                        }
                        else -> it!!.name.asString()
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> continue@parameters
                    is MethodBridgeValueParameter.KotlinResultOutParameter -> "result"
                }

                append(label)
                append(":")
            }

            append(")")
        }.mangledSequence {
            // "foo(label:)" -> "foo(label_:)"
            // "foo()" -> "foo_()"
            insert(lastIndex - 1, '_')
        }
    }

    override fun getPropertyName(property: PropertyDescriptor): String = propertyNames.getOrPut(property) {
        assert(mapper.isBaseProperty(property))
        assert(mapper.isObjCProperty(property))

        StringBuilder().apply {
            append(property.name.asString())
        }.mangledSequence {
            append('_')
        }
    }

    override fun getObjectInstanceSelector(descriptor: ClassDescriptor): String {
        assert(descriptor.kind == ClassKind.OBJECT)

        return objectInstanceSelectors.getOrPut(descriptor) {
            val name = descriptor.name.asString().decapitalize().mangleIfSpecialFamily("get")

            StringBuilder(name).mangledBySuffixUnderscores()
        }
    }

    override fun getEnumEntrySelector(descriptor: ClassDescriptor): String {
        assert(descriptor.kind == ClassKind.ENUM_ENTRY)

        return enumEntrySelectors.getOrPut(descriptor) {
            // FOO_BAR_BAZ -> fooBarBaz:
            val name = descriptor.name.asString().split('_').mapIndexed { index, s ->
                val lower = s.toLowerCase()
                if (index == 0) lower else lower.capitalize()
            }.joinToString("").mangleIfSpecialFamily("the")

            StringBuilder(name).mangledBySuffixUnderscores()
        }
    }

    override fun getTypeParameterName(typeParameterDescriptor: TypeParameterDescriptor): String {
        return genericTypeParameterNameMapping.getOrPut(typeParameterDescriptor) {
            StringBuilder().apply {
                append(typeParameterDescriptor.name.asString())
            }.mangledSequence {
                append('_')
            }
        }
    }

    init {
        val any = builtIns.any

        predefinedClassNames.forEach { descriptor, name ->
            objCClassNames.forceAssign(descriptor, name.objCName)
            swiftClassAndProtocolNames.forceAssign(descriptor, name.swiftName)
        }

        fun ClassDescriptor.method(name: String) =
                this.unsubstitutedMemberScope.getContributedFunctions(
                        Name.identifier(name),
                        NoLookupLocation.FROM_BACKEND
                ).single()

        val hashCode = any.method("hashCode")
        val toString = any.method("toString")
        val equals = any.method("equals")

        methodSelectors.forceAssign(hashCode, "hash")
        methodSwiftNames.forceAssign(hashCode, "hash()")

        methodSelectors.forceAssign(toString, "description")
        methodSwiftNames.forceAssign(toString, "description()")

        methodSelectors.forceAssign(equals, "isEqual:")
        methodSwiftNames.forceAssign(equals, "isEqual(_:)")
    }

    private fun FunctionDescriptor.getMangledName(forSwift: Boolean): String {
        if (this is ConstructorDescriptor) {
            return if (this.constructedClass.isArray && !forSwift) "array" else "init"
        }

        val candidate = when (this) {
            is PropertyGetterDescriptor -> this.correspondingProperty.name.asString()
            is PropertySetterDescriptor -> "set${this.correspondingProperty.name.asString().capitalize()}"
            else -> this.name.asString()
        }

        return candidate.mangleIfSpecialFamily("do")
    }

    private fun String.mangleIfSpecialFamily(prefix: String): String {
        val trimmed = this.dropWhile { it == '_' }
        for (family in listOf("alloc", "copy", "mutableCopy", "new", "init")) {
            if (trimmed.startsWithWords(family)) {
                // Then method can be detected as having special family by Objective-C compiler.
                // mangle the name:
                return prefix + this.capitalize()
            }
        }

        // TODO: handle clashes with NSObject methods etc.

        return this
    }

    private fun String.startsWithWords(words: String) = this.startsWith(words) &&
            (this.length == words.length || !this[words.length].isLowerCase())

    private inner class GenericTypeParameterNameMapping {
        private val elementToName = mutableMapOf<TypeParameterDescriptor, String>()
        private val typeParameterNameClassOverrides = mutableMapOf<ClassDescriptor, MutableSet<String>>()

        fun reserved(name: String): Boolean {
            return name in reservedNames
        }

        fun getOrPut(element: TypeParameterDescriptor, nameCandidates: () -> Sequence<String>): String {
            getIfAssigned(element)?.let { return it }

            nameCandidates().forEach {
                if (tryAssign(element, it)) {
                    return it
                }
            }

            error("name candidates run out")
        }

        private fun tryAssign(element: TypeParameterDescriptor, name: String): Boolean {
            if (element in elementToName) error(element)

            if (reserved(name)) return false

            if (!validName(element, name)) return false

            assignName(element, name)

            return true
        }

        private fun assignName(element: TypeParameterDescriptor, name: String) {
            if (!local) {
                elementToName[element] = name
            }
            classNameSet(element).add(name)
        }

        private fun validName(element: TypeParameterDescriptor, name: String): Boolean {
            assert(element.containingDeclaration is ClassDescriptor)

            val nameSet = classNameSet(element)
            return !objCClassNames.nameExists(name) && !objCProtocolNames.nameExists(name) && name !in nameSet
        }

        private fun classNameSet(element: TypeParameterDescriptor): MutableSet<String> {
            return typeParameterNameClassOverrides.getOrPut(element.containingDeclaration as ClassDescriptor) {
                mutableSetOf()
            }
        }

        private fun getIfAssigned(element: TypeParameterDescriptor): String? = elementToName[element]

        private val reservedNames = setOf("id", "NSObject", "NSArray", "NSCopying", "NSNumber", "NSInteger",
                "NSUInteger", "NSString", "NSSet", "NSDictionary", "NSMutableArray", "int", "unsigned", "short",
                "char", "long", "float", "double", "int32_t", "int64_t", "int16_t", "int8_t", "unichar")
    }

    private abstract inner class Mapping<in T : Any, N>() {
        private val elementToName = mutableMapOf<T, N>()
        private val nameToElements = mutableMapOf<N, MutableList<T>>()

        abstract fun conflict(first: T, second: T): Boolean
        open fun reserved(name: N) = false
        fun getOrPut(element: T, nameCandidates: () -> Sequence<N>): N {
            getIfAssigned(element)?.let { return it }

            nameCandidates().forEach {
                if (tryAssign(element, it)) {
                    return it
                }
            }

            error("name candidates run out")
        }

        fun nameExists(name: N) = nameToElements.containsKey(name)

        private fun getIfAssigned(element: T): N? = elementToName[element]

        private fun tryAssign(element: T, name: N): Boolean {
            if (element in elementToName) error(element)

            if (reserved(name)) return false

            if (nameToElements[name].orEmpty().any { conflict(element, it) }) {
                return false
            }

            if (!local) {
                nameToElements.getOrPut(name) { mutableListOf() } += element

                elementToName[element] = name
            }

            return true
        }

        fun forceAssign(element: T, name: N) {
            if (name in nameToElements || element in elementToName) error(element)

            nameToElements[name] = mutableListOf(element)
            elementToName[element] = name
        }
    }

}

private inline fun StringBuilder.mangledSequence(crossinline mangle: StringBuilder.() -> Unit) =
        generateSequence(this.toString()) {
            this@mangledSequence.mangle()
            this@mangledSequence.toString()
        }

private fun StringBuilder.mangledBySuffixUnderscores() = this.mangledSequence { append("_") }

private fun ObjCExportMapper.canHaveCommonSubtype(first: ClassDescriptor, second: ClassDescriptor): Boolean {
    if (first.isSubclassOf(second) || second.isSubclassOf(first)) {
        return true
    }

    if (first.isFinalClass || second.isFinalClass) {
        return false
    }

    return first.isInterface || second.isInterface
}

private fun ObjCExportMapper.canBeInheritedBySameClass(
        first: CallableMemberDescriptor,
        second: CallableMemberDescriptor
): Boolean {
    if (this.isTopLevel(first) || this.isTopLevel(second)) {
        return this.isTopLevel(first) && this.isTopLevel(second) &&
                first.source.containingFile == second.source.containingFile
    }

    val firstClass = this.getClassIfCategory(first) ?: first.containingDeclaration as ClassDescriptor
    val secondClass = this.getClassIfCategory(second) ?: second.containingDeclaration as ClassDescriptor

    if (first is ConstructorDescriptor) {
        return firstClass == secondClass || second !is ConstructorDescriptor && firstClass.isSubclassOf(secondClass)
    }

    if (second is ConstructorDescriptor) {
        return secondClass == firstClass || first !is ConstructorDescriptor && secondClass.isSubclassOf(firstClass)
    }

    return canHaveCommonSubtype(firstClass, secondClass)
}

private fun ObjCExportMapper.canHaveSameSelector(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
    assert(isBaseMethod(first))
    assert(isBaseMethod(second))

    if (!canBeInheritedBySameClass(first, second)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category method.
        return false
    }

    if (first.name != second.name) {
        return false
    }
    if (first.extensionReceiverParameter?.type != second.extensionReceiverParameter?.type) {
        return false
    }

    if (first is PropertySetterDescriptor && second is PropertySetterDescriptor) {
        // Methods should merge in any common subclass as it can't have two properties with same name.
    } else if (first.valueParameters.map { it.type } == second.valueParameters.map { it.type }) {
        // Methods should merge in any common subclasses since they have the same signature.
    } else {
        return false
    }

    // Check if methods have the same bridge (and thus the same ABI):
    return bridgeMethod(first) == bridgeMethod(second)
}

private fun ObjCExportMapper.canHaveSameName(first: PropertyDescriptor, second: PropertyDescriptor): Boolean {
    assert(isBaseProperty(first))
    assert(isObjCProperty(first))
    assert(isBaseProperty(second))
    assert(isObjCProperty(second))

    if (!canBeInheritedBySameClass(first, second)) {
        return true
    }

    if (first.dispatchReceiverParameter == null || second.dispatchReceiverParameter == null) {
        // I.e. any is category property.
        return false
    }

    return bridgePropertyType(first) == bridgePropertyType(second)
}

internal val ModuleDescriptor.namePrefix: String get() {
    if (this.isKonanStdlib()) return "Kotlin"

    // <fooBar> -> FooBar
    val moduleName = this.name.asString()
        .let { it.substring(1, it.lastIndex) }
        .capitalize()
        .replace('-', '_')

    val uppers = moduleName.filterIndexed { index, character -> index == 0 || character.isUpperCase() }
    if (uppers.length >= 3) return uppers

    return moduleName
}
