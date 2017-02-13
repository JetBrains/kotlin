package kotlinx.cinterop

/**
 * The type of C function which can be constructed from the appropriate Kotlin function without using any adapter.
 */
abstract class CTriviallyAdaptedFunctionType<F : Function<*>> : CAdaptedFunctionType<F> {
    @konan.internal.Intrinsic
    override final fun fromStatic(function: F): NativePtr =
            throw Error("CTriviallyAdaptedFunctionType.fromStatic is called virtually")
}
