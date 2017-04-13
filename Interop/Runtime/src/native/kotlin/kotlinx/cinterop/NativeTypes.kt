package kotlinx.cinterop

import konan.internal.Intrinsic

class NativePtr private constructor() {
    @Intrinsic external operator fun plus(offset: Long): NativePtr

    @Intrinsic external fun toLong(): Long

    override fun equals(other: Any?) = (other is NativePtr) && konan.internal.areEqualByValue(this, other)

    override fun hashCode() = this.toLong().hashCode()

    override fun toString() = this.toLong().toString() // TODO: format as hex.
}

inline val nativeNullPtr: NativePtr
    get() = getNativeNullPtr()

@Intrinsic external fun getNativeNullPtr(): NativePtr

fun <T : CVariable> typeOf(): CVariable.Type = throw Error("typeOf() is called with erased argument")

/**
 * Returns interpretation of entity with given pointer, or `null` if it is null.
 *
 * @param T must not be abstract
 */
@Intrinsic external fun <T : NativePointed> interpretNullablePointed(ptr: NativePtr): T?

@Intrinsic external fun <T : CPointed> interpretCPointer(rawValue: NativePtr): CPointer<T>?

@Intrinsic external fun NativePointed.getRawPointer(): NativePtr

@Intrinsic external fun CPointer<*>.getRawValue(): NativePtr

inline fun <reified T : CAdaptedFunctionType<*>> CAdaptedFunctionType.Companion.getInstanceOf(): T =
        TODO("CAdaptedFunctionType.getInstanceOf 11")

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=$rawValue)"

/**
 * Returns a pointer to `T`-typed C function which calls given Kotlin *static* function.
 * @see CAdaptedFunctionType.fromStatic
 */
// TODO: This function is not inline, whereas the same name function in JvmTypes.kt 
// is inline. We can't make it inline here, because native interop lowering 
// expects to find and transform it. And native interop lowering is done after
// inline expansions.
fun <F : Function<*>, T : CAdaptedFunctionType<F>> staticCFunction(body: F): CFunctionPointer<T> {
    val type = CAdaptedFunctionType.getInstanceOf<T>()
    return interpretPointed<CFunction<T>>(type.fromStatic(body)).ptr
}

