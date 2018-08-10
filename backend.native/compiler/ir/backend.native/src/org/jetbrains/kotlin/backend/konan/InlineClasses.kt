/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.irasdescriptors.containsNull
import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable

fun IrType.getInlinedClass(): IrClass? = IrTypeInlineClassesSupport.getInlinedClass(this)

fun IrType.isInlined(): Boolean = IrTypeInlineClassesSupport.isInlined(this)
fun IrClass.isInlined(): Boolean = IrTypeInlineClassesSupport.isInlined(this)

fun KotlinType.getInlinedClass(): ClassDescriptor? = KotlinTypeInlineClassesSupport.getInlinedClass(this)

fun ClassDescriptor.isInlined(): Boolean = KotlinTypeInlineClassesSupport.isInlined(this)

fun KotlinType.unwrapInlinedClasses() = KotlinTypeInlineClassesSupport.unwrappingInlinedClasses(this).last()

internal inline fun <R> KotlinType.unwrapToPrimitiveOrReference(
        eachInlinedClass: (inlinedClass: ClassDescriptor, nullable: Boolean) -> Unit,
        ifPrimitive: (primitiveType: KonanPrimitiveType, nullable: Boolean) -> R,
        ifReference: (type: KotlinType) -> R
): R = KotlinTypeInlineClassesSupport.unwrapToPrimitiveOrReference(this, eachInlinedClass, ifPrimitive, ifReference)

// TODO: consider renaming to `isReference`.
fun KotlinType.binaryTypeIsReference(): Boolean = this.computePrimitiveBinaryTypeOrNull() == null
fun IrType.binaryTypeIsReference(): Boolean = this.computePrimitiveBinaryTypeOrNull() == null

fun KotlinType.computePrimitiveBinaryTypeOrNull(): PrimitiveBinaryType? =
        this.computeBinaryType().primitiveBinaryTypeOrNull()

fun KotlinType.computeBinaryType(): BinaryType<ClassDescriptor> = KotlinTypeInlineClassesSupport.computeBinaryType(this)

fun IrType.computePrimitiveBinaryTypeOrNull(): PrimitiveBinaryType? =
        this.computeBinaryType().primitiveBinaryTypeOrNull()

fun IrType.computeBinaryType(): BinaryType<IrClass> = IrTypeInlineClassesSupport.computeBinaryType(this)

fun IrClass.inlinedClassIsNullable(): Boolean = this.defaultType.makeNullable().getInlinedClass() == this // TODO: optimize
fun IrClass.isUsedAsBoxClass(): Boolean = IrTypeInlineClassesSupport.isUsedAsBoxClass(this)

/**
 * Most "underlying" user-visible non-reference type.
 * It is visible as inlined to compiler for simplicity, and wraps internal [ValueClass].
 */
enum class KonanPrimitiveType(val classId: ClassId) {
    BOOLEAN(PrimitiveType.BOOLEAN),
    CHAR(PrimitiveType.CHAR),
    BYTE(PrimitiveType.BYTE),
    SHORT(PrimitiveType.SHORT),
    INT(PrimitiveType.INT),
    LONG(PrimitiveType.LONG),
    FLOAT(PrimitiveType.FLOAT),
    DOUBLE(PrimitiveType.DOUBLE),
    NON_NULL_NATIVE_PTR(ClassId.topLevel(KonanBuiltIns.FqNames.nonNullNativePtr.toSafe()))

    ;

    constructor(primitiveType: PrimitiveType) : this(ClassId.topLevel(primitiveType.typeFqName))

    val fqName: FqNameUnsafe get() = this.classId.asSingleFqName().toUnsafe()

    companion object {
        val byFqName = KonanPrimitiveType.values().associateBy { it.fqName }
    }
}

internal abstract class InlineClassesSupport<Class : Any, Type : Any> {
    protected abstract fun isNullable(type: Type): Boolean
    protected abstract fun makeNullable(type: Type): Type
    protected abstract fun erase(type: Type): Class
    protected abstract fun computeFullErasure(type: Type): Sequence<Class>
    protected abstract fun getFqName(clazz: Class): FqNameUnsafe
    protected abstract fun hasInlineModifier(clazz: Class): Boolean
    protected abstract fun getNativePointedSuperclass(clazz: Class): Class?
    protected abstract fun getInlinedClassUnderlyingType(clazz: Class): Type

    @JvmName("classIsInlined")
    fun isInlined(clazz: Class): Boolean = getInlinedClass(clazz) != null
    fun isInlined(type: Type): Boolean = getInlinedClass(type) != null

    fun isUsedAsBoxClass(clazz: Class) = getInlinedClass(clazz) == clazz // To handle NativePointed subclasses.

    fun getInlinedClass(type: Type): Class? =
            getInlinedClass(erase(type), isNullable(type))

    private fun getInlinedClass(erased: Class, isNullable: Boolean): Class? {
        val inlinedClass = getInlinedClass(erased) ?: return null
        return if (!isNullable || representationIsNonNullReferenceOrPointer(inlinedClass)) {
            inlinedClass
        } else {
            null
        }
    }

    tailrec fun representationIsNonNullReferenceOrPointer(clazz: Class): Boolean {
        val valueClass = ValueClass.fqNameToValueClass[getFqName(clazz)]
        if (valueClass != null) {
            return valueClass == ValueClass.NON_NULL_POINTER
        }

        val inlinedClass = getInlinedClass(clazz) ?: return true

        val underlyingType = getInlinedClassUnderlyingType(inlinedClass)
        return if (isNullable(underlyingType)) {
            false
        } else {
            representationIsNonNullReferenceOrPointer(erase(underlyingType))
        }
    }

    @JvmName("classGetInlinedClass")
    private fun getInlinedClass(clazz: Class): Class? =
            if (hasInlineModifier(clazz) || getFqName(clazz) in implicitInlineClasses) {
                clazz
            } else {
                getNativePointedSuperclass(clazz)
            }

    fun unwrapInlinedClass(type: Type): Type? {
        val inlinedClass = getInlinedClass(type) ?: return null
        val underlyingType = getInlinedClassUnderlyingType(inlinedClass)
        return if (isNullable(type)) makeNullable(underlyingType) else underlyingType
    }

    inline fun <R> unwrapToPrimitiveOrReference(
            type: Type,
            eachInlinedClass: (inlinedClass: Class, nullable: Boolean) -> Unit,
            ifPrimitive: (primitiveType: KonanPrimitiveType, nullable: Boolean) -> R,
            ifReference: (type: Type) -> R
    ): R {
        var currentType: Type = type

        while (true) {
            val inlinedClass = getInlinedClass(currentType)
            if (inlinedClass == null) {
                return ifReference(currentType)
            }

            KonanPrimitiveType.byFqName[getFqName(inlinedClass)]?.let { primitiveType ->
                return ifPrimitive(primitiveType, isNullable(type))
            }

            eachInlinedClass(inlinedClass, isNullable(type))
            currentType = unwrapInlinedClass(currentType)!!
        }
    }

    // TODO: optimize.
    fun unwrappingInlinedClasses(type: Type): Sequence<Type> = generateSequence(type, { unwrapInlinedClass(it) })

    // TODO: optimize.
    fun computeBinaryType(type: Type): BinaryType<Class> {
        val erased = erase(type)
        val valueClass = ValueClass.fqNameToValueClass[getFqName(erased)]
        if (valueClass != null) {
            if (isNullable(type) && valueClass != ValueClass.NON_NULL_POINTER) {
                error("${valueClass.fqName} can't be nullable")
            }
            return valueClass.binaryType
        }

        val inlinedClass = getInlinedClass(erased, isNullable(type)) ?: return createReferenceBinaryType(type)
        val underlyingBinaryType = computeBinaryType(getInlinedClassUnderlyingType(inlinedClass))
        return if (isNullable(type) && underlyingBinaryType is BinaryType.Reference) {
            BinaryType.Reference(underlyingBinaryType.types, true)
        } else {
            underlyingBinaryType
        }
    }

    private fun createReferenceBinaryType(type: Type): BinaryType.Reference<Class> =
            BinaryType.Reference(computeFullErasure(type), true)
}

private val implicitInlineClasses =
        (KonanPrimitiveType.values().map { it.fqName } +
                KonanBuiltIns.FqNames.nativePtr +
                InteropBuiltIns.FqNames.cPointer).toSet()

private enum class ValueClass(val fqName: FqNameUnsafe, val binaryType: BinaryType.Primitive) {

    BOOLEAN("konan.internal.BooleanValue", PrimitiveBinaryType.BOOLEAN),
    BYTE("konan.internal.ByteValue", PrimitiveBinaryType.BYTE),
    SHORT("konan.internal.ShortValue", PrimitiveBinaryType.SHORT),
    INT("konan.internal.IntValue", PrimitiveBinaryType.INT),
    LONG("konan.internal.LongValue", PrimitiveBinaryType.LONG),
    FLOAT("konan.internal.FloatValue", PrimitiveBinaryType.FLOAT),
    DOUBLE("konan.internal.DoubleValue", PrimitiveBinaryType.DOUBLE),
    NON_NULL_POINTER("konan.internal.NotNullPointerValue", PrimitiveBinaryType.POINTER)

    ;

    constructor(fqName: String, primitiveBinaryType: PrimitiveBinaryType) :
            this(FqNameUnsafe(fqName), primitiveBinaryType)

    constructor(fqName: FqNameUnsafe, primitiveBinaryType: PrimitiveBinaryType) :
            this(fqName, BinaryType.Primitive(primitiveBinaryType))

    companion object {
        val fqNameToValueClass = ValueClass.values().associateBy { it.fqName }
    }
}

private fun IrClass.getAllSuperClassifiers(): List<IrClass> = listOf(this) + this.superTypes.flatMap { (it.classifierOrFail.owner as IrClass).getAllSuperClassifiers() }

internal object KotlinTypeInlineClassesSupport : InlineClassesSupport<ClassDescriptor, KotlinType>() {

    override fun isNullable(type: KotlinType): Boolean = type.isNullable()
    override fun makeNullable(type: KotlinType): KotlinType = type.makeNullable()
    override tailrec fun erase(type: KotlinType): ClassDescriptor {
        val descriptor = type.constructor.declarationDescriptor
        return if (descriptor is ClassDescriptor) {
            descriptor
        } else {
            erase(type.constructor.supertypes.first())
        }
    }

    override fun computeFullErasure(type: KotlinType): Sequence<ClassDescriptor> {
        val classifier = type.constructor.declarationDescriptor
        return if (classifier is ClassDescriptor) sequenceOf(classifier)
        else type.constructor.supertypes.asSequence().flatMap { computeFullErasure(it) }
    }

    override fun getFqName(clazz: ClassDescriptor): FqNameUnsafe = clazz.fqNameUnsafe
    override fun hasInlineModifier(clazz: ClassDescriptor): Boolean = clazz.isInline

    override fun getNativePointedSuperclass(clazz: ClassDescriptor): ClassDescriptor? = clazz.getAllSuperClassifiers()
            .firstOrNull { it.fqNameUnsafe == InteropBuiltIns.FqNames.nativePointed } as ClassDescriptor?

    override fun getInlinedClassUnderlyingType(clazz: ClassDescriptor): KotlinType =
            clazz.unsubstitutedPrimaryConstructor!!.valueParameters.single().type
}

private object IrTypeInlineClassesSupport : InlineClassesSupport<IrClass, IrType>() {

    override fun isNullable(type: IrType): Boolean = type.containsNull()

    override fun makeNullable(type: IrType): IrType = type.makeNullable()

    override tailrec fun erase(type: IrType): IrClass {
        val classifier = type.classifierOrFail
        return when (classifier) {
            is IrClassSymbol -> classifier.owner
            is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
            else -> error(classifier)
        }
    }

    override fun computeFullErasure(type: IrType): Sequence<IrClass> {
        val classifier = type.classifierOrFail
        return when (classifier) {
            is IrClassSymbol -> sequenceOf(classifier.owner)
            is IrTypeParameterSymbol -> classifier.owner.superTypes.asSequence().flatMap { computeFullErasure(it) }
            else -> error(classifier)
        }
    }

    override fun getFqName(clazz: IrClass): FqNameUnsafe = clazz.fqNameSafe.toUnsafe()
    override fun hasInlineModifier(clazz: IrClass): Boolean = clazz.descriptor.isInline

    override fun getNativePointedSuperclass(clazz: IrClass): IrClass? = clazz.getAllSuperClassifiers()
            .firstOrNull { it.fqNameSafe.toUnsafe() == InteropBuiltIns.FqNames.nativePointed }

    override fun getInlinedClassUnderlyingType(clazz: IrClass): IrType =
            clazz.constructors.first { it.isPrimary }.valueParameters.single().type

}