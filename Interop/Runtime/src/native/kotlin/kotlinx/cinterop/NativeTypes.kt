package kotlinx.cinterop

import konan.internal.getNativeNullPtr
import konan.internal.Intrinsic

typealias NativePtr = konan.internal.NativePtr

inline val nativeNullPtr: NativePtr
    get() = getNativeNullPtr()

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

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=$rawValue)"

@Intrinsic external fun <R> staticCFunction(function: () -> R): CPointer<CFunction<() -> R>>

@Intrinsic external fun <P1, R> staticCFunction(function: (P1) -> R): CPointer<CFunction<(P1) -> R>>

@Intrinsic external fun <P1, P2, R> staticCFunction(function: (P1, P2) -> R): CPointer<CFunction<(P1, P2) -> R>>

@Intrinsic external fun <P1, P2, P3, R> staticCFunction(function: (P1, P2, P3) -> R): CPointer<CFunction<(P1, P2, P3) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, R> staticCFunction(function: (P1, P2, P3, P4) -> R): CPointer<CFunction<(P1, P2, P3, P4) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, R> staticCFunction(function: (P1, P2, P3, P4, P5) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>>

@Intrinsic external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>>