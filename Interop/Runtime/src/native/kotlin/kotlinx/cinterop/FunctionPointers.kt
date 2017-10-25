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

package kotlinx.cinterop

import konan.internal.Intrinsic
import konan.internal.ExportForCompiler

@Intrinsic external operator fun <R> CPointer<CFunction<() -> R>>.invoke(): R

@Intrinsic external operator fun <P1, R> CPointer<CFunction<(P1) -> R>>.invoke(p1: P1): R

@Intrinsic external operator fun <P1, P2, R> CPointer<CFunction<(P1, P2) -> R>>.invoke(p1: P1, p2: P2): R

@Intrinsic external operator fun <P1, P2, P3, R> CPointer<CFunction<(P1, P2, P3) -> R>>.invoke(p1: P1, p2: P2, p3: P3): R

@Intrinsic external operator fun <P1, P2, P3, P4, R> CPointer<CFunction<(P1, P2, P3, P4) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, R> CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21): R

@Intrinsic external operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10, p11: P11, p12: P12, p13: P13, p14: P14, p15: P15, p16: P16, p17: P17, p18: P18, p19: P19, p20: P20, p21: P21, p22: P22): R

@ExportForCompiler
private fun invokeImplUnitRet(ptr: COpaquePointer, vararg args: Any?): Unit = memScoped {
    callWithVarargs(ptr.rawValue, nativeNullPtr, FFI_TYPE_KIND_VOID, args, null, memScope)
}

@ExportForCompiler
private fun invokeImplBooleanRet(ptr: COpaquePointer, vararg args: Any?): Boolean =
        invokeImplByteRet(ptr, *args).toBoolean()

@ExportForCompiler
private fun invokeImplByteRet(ptr: COpaquePointer, vararg args: Any?): Byte = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<ByteVar>(ByteVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_SINT8, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplShortRet(ptr: COpaquePointer, vararg args: Any?): Short = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<ShortVar>(ShortVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_SINT16, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplIntRet(ptr: COpaquePointer, vararg args: Any?): Int = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<IntVar>(IntVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_SINT32, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplLongRet(ptr: COpaquePointer, vararg args: Any?): Long = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<LongVar>(LongVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_SINT64, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplFloatRet(ptr: COpaquePointer, vararg args: Any?): Float = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<FloatVar>(FloatVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_FLOAT, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplDoubleRet(ptr: COpaquePointer, vararg args: Any?): Double = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<DoubleVar>(DoubleVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_DOUBLE, args, null, memScope)
    resultBuffer.value
}

@ExportForCompiler
private fun invokeImplPointerRet(ptr: COpaquePointer, vararg args: Any?): COpaquePointer? = memScoped {
    val resultBuffer = allocFfiReturnValueBuffer<COpaquePointerVar>(COpaquePointerVar)
    callWithVarargs(ptr.rawValue, resultBuffer.rawPtr, FFI_TYPE_KIND_POINTER, args, null, memScope)
    resultBuffer.value
}
