/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.containsNull
import org.jetbrains.kotlin.backend.konan.ir.getSuperClassNotAny
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable

/**
 * TODO: there is [IrType::getInlinedClass] in [org.jetbrains.kotlin.ir.util] which isn't compatible with
 * Native's implementation. Please take a look while commonization.
 */
fun IrType.getInlinedClassNative(): IrClass? = IrTypeInlineClassesSupport.getInlinedClass(this)

/**
 * TODO: there is [IrType::isInlined] in [org.jetbrains.kotlin.ir.util] which isn't compatible with
 * Native's implementation. Please take a look while commonization.
 */
fun IrType.isInlinedNative(): Boolean = IrTypeInlineClassesSupport.isInlined(this)
fun IrClass.isInlined(): Boolean = IrTypeInlineClassesSupport.isInlined(this)

fun KotlinType.getInlinedClass(): ClassDescriptor? = KotlinTypeInlineClassesSupport.getInlinedClass(this)

fun ClassDescriptor.isInlined(): Boolean = KotlinTypeInlineClassesSupport.isInlined(this)

fun KotlinType.binaryRepresentationIsNullable() = KotlinTypeInlineClassesSupport.representationIsNullable(this)

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

fun IrClass.inlinedClassIsNullable(): Boolean = this.defaultType.makeNullable().getInlinedClassNative() == this // TODO: optimize
fun IrClass.isUsedAsBoxClass(): Boolean = IrTypeInlineClassesSupport.isUsedAsBoxClass(this)

/**
 * Most "underlying" user-visible non-reference type.
 * It is visible as inlined to compiler for simplicity.
 */
enum class KonanPrimitiveType(val classId: ClassId, val binaryType: BinaryType.Primitive) {
    BOOLEAN(PrimitiveType.BOOLEAN, PrimitiveBinaryType.BOOLEAN),
    CHAR(PrimitiveType.CHAR, PrimitiveBinaryType.SHORT),
    BYTE(PrimitiveType.BYTE, PrimitiveBinaryType.BYTE),
    SHORT(PrimitiveType.SHORT, PrimitiveBinaryType.SHORT),
    INT(PrimitiveType.INT, PrimitiveBinaryType.INT),
    LONG(PrimitiveType.LONG, PrimitiveBinaryType.LONG),
    FLOAT(PrimitiveType.FLOAT, PrimitiveBinaryType.FLOAT),
    DOUBLE(PrimitiveType.DOUBLE, PrimitiveBinaryType.DOUBLE),
    NON_NULL_NATIVE_PTR(ClassId.topLevel(KonanFqNames.nonNullNativePtr.toSafe()), PrimitiveBinaryType.POINTER)

    ;

    constructor(primitiveType: PrimitiveType, primitiveBinaryType: PrimitiveBinaryType)
            : this(ClassId.topLevel(primitiveType.typeFqName), primitiveBinaryType)

    constructor(classId: ClassId, primitiveBinaryType: PrimitiveBinaryType)
            : this(classId, BinaryType.Primitive(primitiveBinaryType))

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
        val konanPrimitiveType = KonanPrimitiveType.byFqName[getFqName(clazz)]
        if (konanPrimitiveType != null) {
            return konanPrimitiveType == KonanPrimitiveType.NON_NULL_NATIVE_PTR
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

            val nullable = isNullable(currentType)

            KonanPrimitiveType.byFqName[getFqName(inlinedClass)]?.let { primitiveType ->
                return ifPrimitive(primitiveType, nullable)
            }

            eachInlinedClass(inlinedClass, nullable)

            val underlyingType = getInlinedClassUnderlyingType(inlinedClass)
            currentType = if (nullable) makeNullable(underlyingType) else underlyingType
        }
    }

    fun representationIsNullable(type: Type): Boolean {
        unwrapToPrimitiveOrReference(
                type,
                eachInlinedClass = { _, nullable -> if (nullable) return true },
                ifPrimitive = { _, nullable -> return nullable },
                ifReference = { return isNullable(it) }
        )
    }

    // TODO: optimize.
    fun computeBinaryType(type: Type): BinaryType<Class> {
        val erased = erase(type)
        val inlinedClass = getInlinedClass(erased, isNullable(type)) ?: return createReferenceBinaryType(type)

        KonanPrimitiveType.byFqName[getFqName(inlinedClass)]?.let {
            return it.binaryType
        }

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
                KonanFqNames.nativePtr +
                InteropFqNames.cPointer).toSet()

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
            .firstOrNull { it.fqNameUnsafe == InteropFqNames.nativePointed } as ClassDescriptor?

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

    override fun getFqName(clazz: IrClass): FqNameUnsafe = clazz.descriptor.fqNameUnsafe
    override fun hasInlineModifier(clazz: IrClass): Boolean = clazz.descriptor.isInline

    override fun getNativePointedSuperclass(clazz: IrClass): IrClass? {
        var superClass: IrClass? = clazz
        while (superClass != null && superClass.descriptor.fqNameUnsafe != InteropFqNames.nativePointed)
            superClass = superClass.getSuperClassNotAny()
        return superClass
    }

    override fun getInlinedClassUnderlyingType(clazz: IrClass): IrType =
            clazz.constructors.firstOrNull { it.isPrimary }?.valueParameters?.single()?.type
                    ?: clazz.declarations.filterIsInstance<IrProperty>().single { it.backingField != null }.backingField!!.type
}
