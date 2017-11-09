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

package org.jetbrains.kotlin.platform

import org.jetbrains.kotlin.builtins.CompanionObjectMapping
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

object JavaToKotlinClassMap : PlatformToKotlinClassMap {

    private val javaToKotlin = HashMap<FqNameUnsafe, ClassId>()
    private val kotlinToJava = HashMap<FqNameUnsafe, ClassId>()

    private val mutableToReadOnly = HashMap<FqNameUnsafe, FqName>()
    private val readOnlyToMutable = HashMap<FqNameUnsafe, FqName>()

    // describes mapping for a java class that has separate readOnly and mutable equivalents in Kotlin
    data class PlatformMutabilityMapping(
            val javaClass: ClassId,
            val kotlinReadOnly: ClassId,
            val kotlinMutable: ClassId
    )

    private inline fun <reified T> mutabilityMapping(kotlinReadOnly: ClassId, kotlinMutable: FqName): PlatformMutabilityMapping {
        val mutableClassId = ClassId(kotlinReadOnly.packageFqName, kotlinMutable.tail(kotlinReadOnly.packageFqName), false)
        return PlatformMutabilityMapping(classId(T::class.java), kotlinReadOnly, mutableClassId)
    }

    val mutabilityMappings = listOf(
            mutabilityMapping<Iterable<*>>(ClassId.topLevel(FQ_NAMES.iterable), FQ_NAMES.mutableIterable),
            mutabilityMapping<Iterator<*>>(ClassId.topLevel(FQ_NAMES.iterator), FQ_NAMES.mutableIterator),
            mutabilityMapping<Collection<*>>(ClassId.topLevel(FQ_NAMES.collection), FQ_NAMES.mutableCollection),
            mutabilityMapping<List<*>>(ClassId.topLevel(FQ_NAMES.list), FQ_NAMES.mutableList),
            mutabilityMapping<Set<*>>(ClassId.topLevel(FQ_NAMES.set), FQ_NAMES.mutableSet),
            mutabilityMapping<ListIterator<*>>(ClassId.topLevel(FQ_NAMES.listIterator), FQ_NAMES.mutableListIterator),
            mutabilityMapping<Map<*, *>>(ClassId.topLevel(FQ_NAMES.map), FQ_NAMES.mutableMap),
            mutabilityMapping<Map.Entry<*, *>>(
                    ClassId.topLevel(FQ_NAMES.map).createNestedClassId(FQ_NAMES.mapEntry.shortName()), FQ_NAMES.mutableMapEntry
            )
    )

    init {
        addTopLevel(Any::class.java, FQ_NAMES.any)
        addTopLevel(String::class.java, FQ_NAMES.string)
        addTopLevel(CharSequence::class.java, FQ_NAMES.charSequence)
        addTopLevel(Throwable::class.java, FQ_NAMES.throwable)
        addTopLevel(Cloneable::class.java, FQ_NAMES.cloneable)
        addTopLevel(Number::class.java, FQ_NAMES.number)
        addTopLevel(Comparable::class.java, FQ_NAMES.comparable)
        addTopLevel(Enum::class.java, FQ_NAMES._enum)
        addTopLevel(Annotation::class.java, FQ_NAMES.annotation)

        for (platformCollection in mutabilityMappings) {
            addMapping(platformCollection)
        }

        for (jvmType in JvmPrimitiveType.values()) {
            add(ClassId.topLevel(jvmType.wrapperFqName),
                ClassId.topLevel(KotlinBuiltIns.getPrimitiveFqName(jvmType.primitiveType)))
        }

        for (classId in CompanionObjectMapping.allClassesWithIntrinsicCompanions()) {
            add(ClassId.topLevel(FqName("kotlin.jvm.internal." + classId.shortClassName.asString() + "CompanionObject")),
                classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT))
        }

        // TODO: support also functions with >= 23 parameters
        for (i in 0..22) {
            add(ClassId.topLevel(FqName("kotlin.jvm.functions.Function" + i)), KotlinBuiltIns.getFunctionClassId(i))

            val kFunction = FunctionClassDescriptor.Kind.KFunction
            val kFun = kFunction.packageFqName.toString() + "." + kFunction.classNamePrefix
            addKotlinToJava(FqName(kFun + i), ClassId.topLevel(FqName(kFun)))
        }

        addKotlinToJava(FQ_NAMES.nothing.toSafe(), classId(Void::class.java))
    }

    /**
     * E.g.
     * java.lang.String -> kotlin.String
     * java.lang.Integer -> kotlin.Int
     * kotlin.jvm.internal.IntCompanionObject -> kotlin.Int.Companion
     * java.util.List -> kotlin.List
     * java.util.Map.Entry -> kotlin.Map.Entry
     * java.lang.Void -> null
     * kotlin.jvm.functions.Function3 -> kotlin.Function3
     */
    fun mapJavaToKotlin(fqName: FqName): ClassId? {
        return javaToKotlin[fqName.toUnsafe()]
    }

    fun mapJavaToKotlin(fqName: FqName, builtIns: KotlinBuiltIns): ClassDescriptor? {
        val kotlinClassId = mapJavaToKotlin(fqName)
        return if (kotlinClassId != null) builtIns.getBuiltInClassByFqName(kotlinClassId.asSingleFqName()) else null
    }

    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.Int -> java.lang.Integer
     * kotlin.Int.Companion -> kotlin.jvm.internal.IntCompanionObject
     * kotlin.Nothing -> java.lang.Void
     * kotlin.IntArray -> null
     * kotlin.Function3 -> kotlin.jvm.functions.Function3
     * kotlin.reflect.KFunction3 -> kotlin.reflect.KFunction
     */
    fun mapKotlinToJava(kotlinFqName: FqNameUnsafe): ClassId? {
        return kotlinToJava[kotlinFqName]
    }

    private fun addMapping(platformMutabilityMapping: PlatformMutabilityMapping) {
        val (javaClassId, readOnlyClassId, mutableClassId) = platformMutabilityMapping
        add(javaClassId, readOnlyClassId)
        addKotlinToJava(mutableClassId.asSingleFqName(), javaClassId)

        val readOnlyFqName = readOnlyClassId.asSingleFqName()
        val mutableFqName = mutableClassId.asSingleFqName()
        mutableToReadOnly.put(mutableClassId.asSingleFqName().toUnsafe(), readOnlyFqName)
        readOnlyToMutable.put(readOnlyFqName.toUnsafe(), mutableFqName)
    }

    private fun add(javaClassId: ClassId, kotlinClassId: ClassId) {
        addJavaToKotlin(javaClassId, kotlinClassId)
        addKotlinToJava(kotlinClassId.asSingleFqName(), javaClassId)
    }

    private fun addTopLevel(javaClass: Class<*>, kotlinFqName: FqNameUnsafe) {
        addTopLevel(javaClass, kotlinFqName.toSafe())
    }

    private fun addTopLevel(javaClass: Class<*>, kotlinFqName: FqName) {
        add(classId(javaClass), ClassId.topLevel(kotlinFqName))
    }

    private fun addJavaToKotlin(javaClassId: ClassId, kotlinClassId: ClassId) {
        javaToKotlin.put(javaClassId.asSingleFqName().toUnsafe(), kotlinClassId)
    }

    private fun addKotlinToJava(kotlinFqNameUnsafe: FqName, javaClassId: ClassId) {
        kotlinToJava.put(kotlinFqNameUnsafe.toUnsafe(), javaClassId)
    }

    fun isJavaPlatformClass(fqName: FqName): Boolean = mapJavaToKotlin(fqName) != null

    fun mapPlatformClass(fqName: FqName, builtIns: KotlinBuiltIns): Collection<ClassDescriptor> {
        val kotlinAnalog = mapJavaToKotlin(fqName, builtIns) ?: return emptySet()

        val kotlinMutableAnalogFqName = readOnlyToMutable[kotlinAnalog.fqNameUnsafe] ?: return setOf(kotlinAnalog)

        return Arrays.asList(kotlinAnalog, builtIns.getBuiltInClassByFqName(kotlinMutableAnalogFqName))
    }

    override fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor> {
        val className = DescriptorUtils.getFqName(classDescriptor)
        return if (className.isSafe)
            mapPlatformClass(className.toSafe(), classDescriptor.builtIns)
        else
            emptySet<ClassDescriptor>()
    }

    fun isMutable(mutable: ClassDescriptor): Boolean = mutableToReadOnly.containsKey(DescriptorUtils.getFqName(mutable))

    fun isMutable(type: KotlinType): Boolean {
        val classDescriptor = TypeUtils.getClassDescriptor(type)
        return classDescriptor != null && isMutable(classDescriptor)
    }

    fun isReadOnly(readOnly: ClassDescriptor): Boolean = readOnlyToMutable.containsKey(DescriptorUtils.getFqName(readOnly))

    fun isReadOnly(type: KotlinType): Boolean {
        val classDescriptor = TypeUtils.getClassDescriptor(type)
        return classDescriptor != null && isReadOnly(classDescriptor)
    }

    fun convertMutableToReadOnly(mutable: ClassDescriptor): ClassDescriptor {
        return convertToOppositeMutability(mutable, mutableToReadOnly, "mutable")
    }

    fun convertReadOnlyToMutable(readOnly: ClassDescriptor): ClassDescriptor {
        return convertToOppositeMutability(readOnly, readOnlyToMutable, "read-only")
    }

    private fun classId(clazz: Class<*>): ClassId {
        assert(!clazz.isPrimitive && !clazz.isArray) { "Invalid class: " + clazz }
        val outer = clazz.declaringClass
        return if (outer == null)
            ClassId.topLevel(FqName(clazz.canonicalName))
        else
            classId(outer).createNestedClassId(Name.identifier(clazz.simpleName))
    }

    private fun convertToOppositeMutability(
            descriptor: ClassDescriptor,
            map: Map<FqNameUnsafe, FqName>,
            mutabilityKindName: String
    ): ClassDescriptor {
        val oppositeClassFqName = map[DescriptorUtils.getFqName(descriptor)] ?: throw IllegalArgumentException("Given class $descriptor is not a $mutabilityKindName collection")
        return descriptor.builtIns.getBuiltInClassByFqName(oppositeClassFqName)
    }
}
