/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData
import org.jetbrains.kotlin.descriptors.runtime.structure.Java16SealedRecordLoader
import org.jetbrains.kotlin.descriptors.runtime.structure.functionClassArity
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.GivenFunctionsMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.compact
import java.io.Serializable
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference.NO_RECEIVER
import kotlin.jvm.internal.KotlinGenericDeclaration
import kotlin.jvm.internal.TypeIntrinsics
import kotlin.metadata.*
import kotlin.metadata.ClassKind
import kotlin.metadata.Modality
import kotlin.metadata.internal.toKmClass
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.localDelegatedProperties
import kotlin.metadata.jvm.moduleName
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.types.DescriptorKType
import org.jetbrains.kotlin.descriptors.ClassKind as DescriptorClassKind
import org.jetbrains.kotlin.descriptors.Modality as DescriptorModality

internal class KClassImpl<T : Any>(
    override val jClass: Class<T>,
) : KDeclarationContainerImpl(), KClass<T>, KTypeParameterOwnerImpl, TypeConstructorMarker, KotlinGenericDeclaration {
    inner class Data : KDeclarationContainerImpl.Data() {
        val kmClass: KmClass? by lazy(PUBLICATION) {
            if (loadMetadataDirectly) {
                val metadata = jClass.getAnnotation(Metadata::class.java)
                if (metadata != null && classId.outerClassId !in CompanionObjectMapping.classIds)
                    (KotlinClassMetadata.readLenient(metadata) as? KotlinClassMetadata.Class)?.kmClass
                else
                    readBuiltinClassMetadata(classId)
            } else {
                val descriptor = descriptor
                if (descriptor is FunctionClassDescriptor) {
                    // There are no special KClass instances for suspend function type classes yet (KT-79225), so all functions are
                    // treated as normal functions (`kotlin.Function{n}`).
                    if (descriptor.functionTypeKind !is FunctionTypeKind.Function)
                        throw KotlinReflectionInternalError("Unsupported function type kind: ${descriptor.functionTypeKind} ($descriptor)")
                    return@lazy createFunctionKmClass(descriptor.arity)
                }
                if (jClass == Cloneable::class.java) {
                    return@lazy createCloneableKmClass()
                }
                (descriptor as? DeserializedClassDescriptor)?.let { descriptor ->
                    descriptor.classProto.toKmClass(descriptor.c.nameResolver)
                }
            }
        }

        val descriptor: ClassDescriptor by ReflectProperties.lazySoft {
            val classId = classId
            val moduleData = data.value.moduleData
            val module = moduleData.module

            @OptIn(ClassIdBasedLocality::class)
            val descriptor =
                if (classId.isLocal && jClass.isAnnotationPresent(Metadata::class.java)) {
                    // If it's a Kotlin local class or anonymous object, deserialize its metadata directly because it cannot be found via
                    // `module.findClassAcrossModuleDependencies`.
                    moduleData.deserialization.deserializeClass(classId)
                } else {
                    module.findClassAcrossModuleDependencies(classId)
                }

            descriptor ?: createSyntheticClassOrFail(classId, moduleData)
        }

        val annotations: List<Annotation> by ReflectProperties.lazySoft {
            val allAnnotations = jClass.annotations
            val declaredAnnotations = jClass.declaredAnnotations
            val hasInheritedAnnotations = allAnnotations.size != declaredAnnotations.size

            val filteredAnnotations = if (!hasInheritedAnnotations) {
                allAnnotations.filterNot { it.annotationClass.java.name in SPECIAL_JVM_ANNOTATION_NAMES }
            } else {
                // For inherited annotations, the annotations declared on subclasses override ones on the base classes, and such "shadowed"
                // annotations are usually not present in Java's getAnnotations().
                // But if
                // - an inherited annotation is also repeatable (either by Java's or Kotlin's @Repeatable),
                // - some classes in super-class hierarchy contain multiple instances of that annotation (thus stored as a container),
                // - some other classes contain single annotation instance (thus stored as is),
                // then both container and single annotation may present in Java's getAnnotations() result, as this method does not
                // unwrap containers for shadowing purposes.
                // So, we need to collect declared annotations from the class and all superclasses, with filtering by "shadowing" rules.

                // although there is no requirement to keep any order, it is still better to keep a logical order of Parent->...->Child
                // as we iterate in reverse order (child to parents), the temporary result order is "reversed"
                val resultReversed = mutableListOf<Annotation>()
                val unwrappedAnnotationClassesHosts = mutableMapOf<KClass<out Annotation>, Class<out Any>>()
                var currentClass: Class<out Any> = jClass
                while (true) {
                    val currentClassAnnotations = currentClass.declaredAnnotations
                    for (i in currentClassAnnotations.size - 1 downTo 0) {
                        val annotation = currentClassAnnotations[i]

                        if (annotation.annotationClass.java.name !in SPECIAL_JVM_ANNOTATION_NAMES &&
                            (currentClass === jClass || annotation.isInheritable)
                        ) {
                            val unwrappedAnnotationClass: KClass<out Annotation> = annotation.unwrappedAnnotationClass
                            val prevHost = unwrappedAnnotationClassesHosts[unwrappedAnnotationClass]
                            if (prevHost == null) {
                                unwrappedAnnotationClassesHosts[unwrappedAnnotationClass] = currentClass
                            }
                            if (prevHost == null || prevHost == currentClass) {
                                resultReversed.add(annotation)
                            }
                        }
                    }

                    currentClass = currentClass.superclass ?: break
                }

                resultReversed.reversed()
            }

            filteredAnnotations.unwrapKotlinRepeatableAnnotations()
        }

        val simpleName: String? by ReflectProperties.lazySoft {
            if (jClass.isAnonymousClass) return@lazySoft null

            val classId = classId
            @OptIn(ClassIdBasedLocality::class)
            when {
                classId.isLocal -> calculateLocalClassName(jClass)
                else -> classId.shortClassName.asString()
            }
        }

        val qualifiedName: String? by ReflectProperties.lazySoft {
            if (jClass.isAnonymousClass) return@lazySoft null

            val classId = classId
            @OptIn(ClassIdBasedLocality::class)
            when {
                classId.isLocal -> null
                else -> classId.asSingleFqName().asString()
            }
        }

        private val Annotation.isInheritable: Boolean
            get() = hasInherited() && !isRepeatableContainerForNonInheritedAnnotation()

        private fun calculateLocalClassName(jClass: Class<*>): String {
            val name = jClass.simpleName
            jClass.enclosingMethod?.let { method ->
                return name.substringAfter(method.name + "$")
            }
            jClass.enclosingConstructor?.let { constructor ->
                return name.substringAfter(constructor.name + "$")
            }
            return name.substringAfter('$')
        }

        @Suppress("UNCHECKED_CAST")
        val constructors: Collection<KFunction<T>> by ReflectProperties.lazySoft {
            if (classKind == ClassKind.INTERFACE || classKind == ClassKind.OBJECT || classKind == ClassKind.COMPANION_OBJECT ||
                classKind == ClassKind.ENUM_ENTRY || jClass.isSynthetic
            ) {
                return@lazySoft emptyList()
            }

            if (useK1Implementation || isClassWithAdditionalConstructorsFromMappedType()) {
                constructorDescriptors.map { descriptor ->
                    DescriptorKFunction(this@KClassImpl, descriptor) as KFunction<T>
                }
            } else if (kmClass != null) {
                constructorsMetadata.map { kmConstructor ->
                    createUnboundConstructor(kmConstructor, this@KClassImpl) as KFunction<T>
                }
            } else if (jClass.isAnnotationPresent(Metadata::class.java)) {
                // In case of a Kotlin synthetic class, there's no KmClass, and there should be no constructors.
                emptyList()
            } else if (!jClass.isAnnotation) {
                jClass.declaredConstructors.mapNotNull { javaConstructor ->
                    JavaKConstructor(this@KClassImpl, javaConstructor, NO_RECEIVER) as KFunction<T>
                }
            } else {
                // Annotation classes do not have a constructor, and Java classes have do not have Kotlin metadata, so we need to create
                // constructors for Java annotation classes manually.
                listOf(JavaAnnotationConstructor(this@KClassImpl) as KFunction<T>)
            }
        }

        // TODO (KT-86101): support `JvmBuiltInsCustomizer.getConstructors` in new implementation.
        private fun isClassWithAdditionalConstructorsFromMappedType(): Boolean =
            kmClass != null && jClass.declaredConstructors.count {
                Modifier.isPublic(it.modifiers) || Modifier.isProtected(it.modifiers)
            } > constructorsMetadata.size

        val nestedClasses: Collection<KClass<*>> by ReflectProperties.lazySoft {
            val kmClass = kmClass
            when {
                kmClass != null -> {
                    val classId = kmClass.name.toClassId()
                    val classLoader = jClass.safeClassLoader
                    kmClass.nestedClasses.mapNotNull { name ->
                        classLoader.loadClass(classId.createNestedClassId(Name.identifier(name)))?.kotlin
                    }
                }
                else -> jClass.declaredClasses.mapNotNull { it.kotlin }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val objectInstance: T? by lazy(PUBLICATION) {
            val kmClass = kmClass
            if (kmClass == null || (kmClass.kind != ClassKind.OBJECT && kmClass.kind != ClassKind.COMPANION_OBJECT))
                return@lazy null

            val field = if (
                kmClass.kind == ClassKind.COMPANION_OBJECT &&
                kmClass.name.toClassId().outerClassId !in CompanionObjectMapping.classIds
            ) {
                // Note that `kmClass.name` cannot be local because local objects are not allowed.
                jClass.enclosingClass.getDeclaredField(kmClass.name.toNonLocalSimpleName())
            } else {
                jClass.getDeclaredField(JvmAbi.INSTANCE_FIELD)
            }
            field.get(null) as T
        }

        val typeParameters: List<KTypeParameter> by ReflectProperties.lazySoft {
            if (useK1Implementation) {
                descriptor.declaredTypeParameters.map { descriptor -> KTypeParameterImpl(this@KClassImpl, descriptor) }
            } else if (kmClass == null) {
                jClass.typeParameters.toKTypeParameters(this@KClassImpl)
            } else {
                typeParameterTable.ownTypeParameters
            }
        }

        internal val typeParameterTable: TypeParameterTable by ReflectProperties.lazySoft {
            if (kmClass == null)
                TypeParameterTable.EMPTY
            else
                TypeParameterTable.create(
                    kmClass!!.typeParameters,
                    (jClass.enclosingClass?.takeIf { kmClass!!.isInner }?.kotlin as? KClassImpl<*>)?.data?.value?.typeParameterTable,
                    this@KClassImpl,
                    jClass.safeClassLoader,
                )
        }

        val supertypes: List<KType> by ReflectProperties.lazySoft {
            if (jClass == Any::class.java) return@lazySoft emptyList()

            if (useK1Implementation) {
                return@lazySoft computeLegacySupertypes()
            }

            val result = ArrayList<KType>()
            val kmTypes = kmClass?.supertypes
            if (kmTypes != null) {
                kmTypes.mapTo(result) { kmType ->
                    val superClassId = (kmType.classifier as? KmClassifier.Class)?.name?.toClassId()
                        ?: throw KotlinReflectionInternalError("Supertype of ${this@KClassImpl} not a class: ${kmType.classifier}")

                    val superJavaClass = jClass.safeClassLoader.loadClass(superClassId)
                        ?: throw KotlinReflectionInternalError("Unsupported superclass of ${this@KClassImpl}: $superClassId")

                    kmType.toKType(jClass.safeClassLoader, typeParameterTable) {
                        if (jClass.superclass == superJavaClass) {
                            jClass.genericSuperclass
                        } else {
                            val index = jClass.interfaces.indexOf(superJavaClass)
                            if (index < 0) throw KotlinReflectionInternalError(
                                "No superclass of ${this@KClassImpl} in Java reflection for $superClassId"
                            )
                            jClass.genericInterfaces[index]
                        }
                    }
                }

                // Compiler adds Cloneable and Serializable supertypes for some builtin classes, see `JvmBuiltInsCustomizer.getSupertypes`.
                if (jClass.isArray) {
                    result += StandardKTypes.CLONEABLE
                }
                val jClassWrappedIfPossible = jClass.wrapperByPrimitive ?: jClass
                if (Serializable::class.java.isAssignableFrom(jClassWrappedIfPossible) && StandardKTypes.SERIALIZABLE !in result &&
                    qualifiedName?.startsWith("kotlin.") == true && (jClass.isArray || JavaToKotlinClassMap.isMappedKotlinClass(classId))
                ) {
                    result += StandardKTypes.SERIALIZABLE
                }
            } else {
                val purelyImplementedSupertype = getPurelyImplementedSupertype(this@KClassImpl)
                for (superClass in listOf(jClass.genericSuperclass, *jClass.genericInterfaces)) {
                    if (superClass == null || superClass == Any::class.java || superClass == purelyImplementedSupertype?.classifier)
                        continue
                    result += superClass.toKType(
                        knownTypeParameters = emptyMap(), nullability = TypeNullability.NOT_NULL, howThisTypeIsUsed = TypeUsage.SUPERTYPE,
                    )
                }
                result.addIfNotNull(purelyImplementedSupertype)
            }

            if (result.all {
                    val klass = it.classifier as? KClassImpl<*>
                    klass != null && (klass.classKind == ClassKind.INTERFACE || klass.classKind == ClassKind.ANNOTATION_CLASS)
                }) {
                result += StandardKTypes.ANY
            }
            result.compact()
        }

        private fun computeLegacySupertypes(): List<KType> {
            val kotlinTypes = descriptor.typeConstructor.supertypes
            val result = ArrayList<KType>(kotlinTypes.size)
            kotlinTypes.mapTo(result) { kotlinType ->
                DescriptorKType(kotlinType) {
                    val superClass = kotlinType.constructor.declarationDescriptor
                    if (superClass !is ClassDescriptor) throw KotlinReflectionInternalError("Supertype not a class: $superClass")

                    val superJavaClass = superClass.toJavaClass()
                        ?: throw KotlinReflectionInternalError("Unsupported superclass of ${this@KClassImpl}: $superClass")

                    if (jClass.superclass == superJavaClass) {
                        jClass.genericSuperclass
                    } else {
                        val index = jClass.interfaces.indexOf(superJavaClass)
                        if (index < 0) throw KotlinReflectionInternalError(
                            "No superclass of ${this@KClassImpl} in Java reflection for $superClass"
                        )
                        jClass.genericInterfaces[index]
                    }
                }
            }
            if (!KotlinBuiltIns.isSpecialClassWithNoSupertypes(descriptor) && result.all {
                    val klass = it.classifier as? KClassImpl<*>
                    klass != null && (klass.classKind == ClassKind.INTERFACE || klass.classKind == ClassKind.ANNOTATION_CLASS)
                }) {
                result += StandardKTypes.ANY
            }
            return result.compact()
        }

        val sealedSubclasses: List<KClass<out T>> by ReflectProperties.lazySoft {
            val classLoader = jClass.safeClassLoader
            val kmClass = kmClass
            val result = when {
                kmClass != null ->
                    kmClass.sealedSubclasses.mapNotNull(classLoader::loadKClass)
                Java16SealedRecordLoader.loadIsSealed(jClass) == true ->
                    Java16SealedRecordLoader.loadGetPermittedSubclasses(jClass)?.map { it.kotlin }.orEmpty()
                else -> emptyList()
            }
            @Suppress("UNCHECKED_CAST")
            result as List<KClass<out T>>
        }

        internal val inlineClassUnderlyingType: KType? by lazy(PUBLICATION) {
            val kmClass = kmClass
            when {
                kmClass == null || !kmClass.isValue ->
                    null
                kmClass.inlineClassUnderlyingType != null ->
                    kmClass.inlineClassUnderlyingType?.toKType(jClass.safeClassLoader, typeParameterTable)
                else -> {
                    val underlyingProperty = kmClass.properties.single {
                        it.name == kmClass.inlineClassUnderlyingPropertyName &&
                                it.contextParameters.isEmpty() && it.receiverParameterType == null
                    }
                    underlyingProperty.returnType.toKType(jClass.safeClassLoader, typeParameterTable)
                }
            }
        }

        // TODO: KT-85727 Reflection: support collections and their subclasses in the new implementation
        val isComplicatedBuiltinSubclass: Boolean by lazy(PUBLICATION) {
            Iterable::class.java.isAssignableFrom(jClass) ||
                    Map::class.java.isAssignableFrom(jClass) ||
                    CharSequence::class.java.isAssignableFrom(jClass) ||
                    Number::class.java.isAssignableFrom(jClass)
        }

        val declaredMemberNames: Set<String> by ReflectProperties.lazySoft(::computeDeclaredMemberNames)

        private val declaredMembersByName: ConcurrentHashMap<String, Collection<ReflectKCallable<*>>>
                by ReflectProperties.lazySoft { ConcurrentHashMap() }

        private val allMembersByName: ConcurrentHashMap<String, Collection<ReflectKCallable<*>>>
                by ReflectProperties.lazySoft { ConcurrentHashMap() }

        private val fakeOverrideMembersByName: ConcurrentHashMap<String, MembersJavaSignatureMap>
                by ReflectProperties.lazySoft { ConcurrentHashMap() }

        fun getDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>> =
            declaredMembersByName.getOrPut(name) { computeDeclaredMembersByName(name) }

        fun getMembersByName(name: String): Collection<ReflectKCallable<*>> =
            allMembersByName.getOrPut(name) { computeMembersByName(name) }

        fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap =
            fakeOverrideMembersByName.getOrPut(name) { computeFakeOverrideMembersForName(this@KClassImpl, name) }

        val declaredMembers: Collection<ReflectKCallable<*>> by ReflectProperties.lazySoft(::computeDeclaredMembers)

        val allMembers: Collection<ReflectKCallable<*>> by ReflectProperties.lazySoft(::computeAllMembers)
    }

    val data = lazy(PUBLICATION) { Data() }

    val descriptor: ClassDescriptor get() = data.value.descriptor

    internal val kmClass: KmClass? get() = data.value.kmClass

    override val annotations: List<Annotation> get() = data.value.annotations

    internal val classId: ClassId get() = RuntimeTypeMapper.mapJvmClassToKotlinClassId(jClass)

    internal val classKind: ClassKind
        get() = kmClass?.kind ?: when {
            jClass.isAnnotation -> ClassKind.ANNOTATION_CLASS
            jClass.isInterface -> ClassKind.INTERFACE
            jClass.isEnum -> ClassKind.ENUM_CLASS
            jClass.superclass.isEnum -> ClassKind.ENUM_ENTRY
            else -> ClassKind.CLASS
        }

    internal val memberScope: MemberScope get() = descriptor.defaultType.memberScope

    internal val staticScope: MemberScope get() = descriptor.staticScope

    override val members: Collection<KCallable<*>> get() = data.value.allMembers

    internal fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap = data.value.getFakeOverrideMembersByName(name)

    val isComplicatedBuiltinSubclass: Boolean get() = data.value.isComplicatedBuiltinSubclass

    override val functionsMetadata: Collection<KmFunction>
        get() = kmClass?.functions.orEmpty()

    override val propertiesMetadata: Collection<KmProperty>
        get() = kmClass?.properties.orEmpty()

    override val constructorsMetadata: Collection<KmConstructor>
        get() = kmClass?.constructors.orEmpty()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = descriptor.constructors

    override fun getProperties(name: Name): Collection<PropertyDescriptor> =
        (memberScope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION) +
                staticScope.getContributedVariables(name, NoLookupLocation.FROM_REFLECTION))

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> =
        memberScope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION) +
                staticScope.getContributedFunctions(name, NoLookupLocation.FROM_REFLECTION)

    override fun getLocalPropertyDescriptor(index: Int): PropertyDescriptor? {
        return (descriptor as? DeserializedClassDescriptor)?.let { descriptor ->
            descriptor.classProto.getExtensionOrNull(JvmProtoBuf.classLocalVariable, index)?.let { proto ->
                deserializeToDescriptor(
                    jClass, LocalDelegatedPropertyFakeContainerSource(this), proto, descriptor.c.nameResolver, descriptor.c.typeTable,
                    descriptor.metadataVersion,
                ) { proto -> loadProperty(proto, loadAnnotationsFromMetadata = true) }
            }
        }
    }

    override fun getLocalPropertyMetadata(index: Int): KmProperty? =
        kmClass?.localDelegatedProperties?.getOrNull(index)

    override val simpleName: String? get() = data.value.simpleName

    override val qualifiedName: String? get() = data.value.qualifiedName

    override val constructors: Collection<KFunction<T>> get() = data.value.constructors

    override val nestedClasses: Collection<KClass<*>> get() = data.value.nestedClasses

    override val objectInstance: T? get() = data.value.objectInstance

    override fun isInstance(value: Any?): Boolean {
        // TODO: use Kotlin semantics for mutable/read-only collections once KT-11754 is supported (see TypeIntrinsics)
        jClass.functionClassArity?.let { arity ->
            return TypeIntrinsics.isFunctionOfArity(value, arity)
        }
        return (jClass.wrapperByPrimitive ?: jClass).isInstance(value)
    }

    override val typeParameters: List<KTypeParameter> get() = data.value.typeParameters

    internal val typeParameterTable: TypeParameterTable get() = data.value.typeParameterTable

    override val supertypes: List<KType> get() = data.value.supertypes

    /**
     * The list of the immediate subclasses if this class is a sealed class, or an empty list otherwise.
     */
    override val sealedSubclasses: List<KClass<out T>> get() = data.value.sealedSubclasses

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    private val modality: Modality
        get() = kmClass?.modality ?: when {
            jClass.isAnnotation || jClass.isEnum -> Modality.FINAL
            Java16SealedRecordLoader.loadIsSealed(jClass) == true -> Modality.SEALED
            Modifier.isAbstract(jClass.modifiers) -> Modality.ABSTRACT
            !Modifier.isFinal(jClass.modifiers) -> Modality.OPEN
            else -> Modality.FINAL
        }

    override val isFinal: Boolean
        get() = modality == Modality.FINAL

    override val isOpen: Boolean
        get() = modality == Modality.OPEN

    override val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT

    override val isSealed: Boolean
        get() = modality == Modality.SEALED

    override val isData: Boolean
        get() = kmClass?.isData == true

    override val isInner: Boolean
        get() = when (val kmClass = kmClass) {
            null -> jClass.declaringClass != null && !Modifier.isStatic(jClass.modifiers)
            else -> kmClass.isInner
        }

    override val isCompanion: Boolean
        get() = kmClass?.kind == ClassKind.COMPANION_OBJECT

    override val isFun: Boolean
        get() = kmClass?.isFunInterface == true

    override val isValue: Boolean
        get() = kmClass?.isValue == true

    internal val isJvmInlineValue: Boolean
        get() = isValue && inlineClassUnderlyingPropertyName != null

    internal val inlineClassUnderlyingPropertyName: String?
        get() = kmClass?.inlineClassUnderlyingPropertyName

    internal val inlineClassUnderlyingType: KType?
        get() = data.value.inlineClassUnderlyingType

    override fun findJavaDeclaration(): GenericDeclaration = jClass

    internal val moduleName: String?
        get() = kmClass?.moduleName

    override fun equals(other: Any?): Boolean =
        other is KClassImpl<*> && javaObjectType == other.javaObjectType

    override fun hashCode(): Int =
        javaObjectType.hashCode()

    override fun toString(): String {
        return "class " + classId.let { classId ->
            val packageFqName = classId.packageFqName
            val packagePrefix = if (packageFqName.isRoot) "" else packageFqName.asString() + "."
            val classSuffix = classId.relativeClassName.asString().replace('.', '$')
            packagePrefix + classSuffix
        }
    }

    private fun createSyntheticClassOrFail(classId: ClassId, moduleData: RuntimeModuleData): ClassDescriptor {
        if (jClass.isSynthetic) {
            // Synthetic classes, either from Java or from Kotlin, have no Kotlin metadata and no reliable way (and probably no use cases)
            // to introspect, so we create an empty synthetic class descriptor for them.
            // This is especially useful for Java lambdas which have names like `JavaClass$$Lambda$4711/1112495601` and are NOT recognized
            // as local or anonymous classes (j.l.Class.isLocalClass/isAnonymousClass return false), which breaks some invariants in the
            // subsequent code in kotlin-reflect if it tries to interpret them as normal anonymous classes and load their members.
            return createSyntheticClass(classId, moduleData)
        }

        when (val kind = ReflectKotlinClass.create(jClass)?.classHeader?.kind) {
            KotlinClassHeader.Kind.FILE_FACADE,
            KotlinClassHeader.Kind.MULTIFILE_CLASS,
            KotlinClassHeader.Kind.MULTIFILE_CLASS_PART,
            KotlinClassHeader.Kind.SYNTHETIC_CLASS,
                ->
                return createSyntheticClass(classId, moduleData)
            KotlinClassHeader.Kind.UNKNOWN -> {
                // Should not happen since ABI-related exception must have happened earlier
                throw KotlinReflectionInternalError("Unknown class: $jClass (kind = $kind)")
            }
            KotlinClassHeader.Kind.CLASS, null -> {
                // Should not happen since a proper Kotlin- or Java-class must have been resolved
                throw KotlinReflectionInternalError("Unresolved class: $jClass (kind = $kind)")
            }
        }
    }

    private fun createSyntheticClass(classId: ClassId, moduleData: RuntimeModuleData): ClassDescriptor =
        ClassDescriptorImpl(
            EmptyPackageFragmentDescriptor(moduleData.module, classId.packageFqName),
            classId.shortClassName,
            DescriptorModality.FINAL,
            DescriptorClassKind.CLASS,
            listOf(moduleData.module.builtIns.any.defaultType),
            SourceElement.NO_SOURCE,
            false,
            moduleData.deserialization.storageManager,
        ).also { descriptor ->
            descriptor.initialize(object : GivenFunctionsMemberScope(moduleData.deserialization.storageManager, descriptor) {
                // Don't declare any functions in this class descriptor, only inherit equals/hashCode/toString from Any.
                override fun computeDeclaredFunctions(): List<FunctionDescriptor> = emptyList()
            }, emptySet(), null)
        }

    companion object {
        private val SPECIAL_JVM_ANNOTATION_NAMES: Set<String> = SpecialJvmAnnotations.SPECIAL_ANNOTATIONS.mapTo(HashSet()) {
            it.asSingleFqName().toString()
        }
    }
}
