
// FILE: test.kt
fun box() {
    val a = 1
    val b = 2
    try {
        throwIfLess(a, b)
    } catch (e: Exception) {
        throwIfLess(a, b)
    }
    throwIfLess(b,a)
}

fun throwIfLess(a: Int, b: Int) {
    if (a<b)
        throw IllegalStateException()
}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess
// test.kt:8 box
// test.kt:9 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:7 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:15 throwIfLess
// test.kt:16 throwIfLess

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:4 $box (12, 4)
// test.kt:5 $box (12, 4)
// test.kt:7 $box (20, 23, 8)
// test.kt:15 $throwIfLess (8, 10, 8, 8, 10, 8)
// test.kt:16 $throwIfLess (14, 14, 8, 14, 14, 8)
// Exceptions.kt:37 $kotlin.IllegalStateException.<init> (34, 34, 4, 4, 41, 34, 34, 4, 4, 41)
// Exceptions.kt:23 $kotlin.RuntimeException.<init> (34, 34, 4, 4, 41, 34, 34, 4, 4, 41)
// Exceptions.kt:16 $kotlin.Exception.<init> (34, 34, 4, 4, 41, 34, 34, 4, 4, 41)
// Throwable.kt:23 $kotlin.Throwable.<init> (32, 38, 27, 27, 43, 32, 38, 27, 27, 43)
// Throwable.kt:18 $kotlin.Throwable.<init> (28, 62, 28, 62)
// Throwable.kt:25 $kotlin.Throwable.<init> (50, 50)
// ExternalWrapper.kt:226 $kotlin.wasm.internal.jsCheckIsNullOrUndefinedAdapter (18, 8, 32, 33, 18, 8, 32, 33, 18, 8, 32, 33)
// Throwable.kt:27 $kotlin.Throwable.<init> (34, 34)
// Throwable.kt:39 $kotlin.Throwable.<init> (69, 69)
// Throwable.kt:49 $kotlin.Throwable.<init> (1, 1)
// Exceptions.kt:20 $kotlin.Exception.<init> (1, 1)
// Exceptions.kt:27 $kotlin.RuntimeException.<init> (1, 1)
// Exceptions.kt:41 $kotlin.IllegalStateException.<init> (1, 1)
// test.kt:6 $box
// test.kt:8 $box (27, 13)
// test.kt:9 $box (20, 23, 8)
// ExceptionHelpers.kt:20 $kotlin.wasm.internal.throwAsJsException (17, 19, 19, 19, 42, 44, 28, 55, 57, 4)
// Throwable.kt:18 $kotlin.Throwable.<get-message>
// TypeInfo.kt:34 $kotlin.wasm.internal.getSimpleName
// TypeInfo.kt:35 $kotlin.wasm.internal.getSimpleName
// TypeInfo.kt:36 $kotlin.wasm.internal.getSimpleName
// TypeInfo.kt:37 $kotlin.wasm.internal.getSimpleName
// TypeInfo.kt:33 $kotlin.wasm.internal.getSimpleName
// TypeInfo.kt:48 $kotlin.wasm.internal.getString (31, 45, 31, 17, 4)
// TypeInfo.kt:49 $kotlin.wasm.internal.getString (27, 41, 27, 13, 4)
// TypeInfo.kt:50 $kotlin.wasm.internal.getString (28, 42, 28, 14, 4)
// TypeInfo.kt:51 $kotlin.wasm.internal.getString (25, 29, 34, 11, 4)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set
// String.kt:149 $kotlin.stringLiteral (11, 4)
// TypeInfo.kt:38 $kotlin.wasm.internal.getSimpleName
// ExternalWrapper.kt:200 $kotlin.wasm.internal.kotlinToJsStringAdapter
// ExternalWrapper.kt:201 $kotlin.wasm.internal.kotlinToJsStringAdapter
// Strings.kt:161 $kotlin.wasm.internal.kotlinToJsStringAdapter
// Strings.kt:296 $kotlin.wasm.internal.kotlinToJsStringAdapter (52, 62, 52, 63)
// String.kt:18 $kotlin.String.<get-length>
// ExternalWrapper.kt:203 $kotlin.wasm.internal.kotlinToJsStringAdapter
// String.kt:150 $kotlin.wasm.internal.kotlinToJsStringAdapter
// String.kt:63 $kotlin.wasm.internal.kotlinToJsStringAdapter
// String.kt:66 $kotlin.wasm.internal.kotlinToJsStringAdapter (15, 8)
// ExternalWrapper.kt:204 $kotlin.wasm.internal.kotlinToJsStringAdapter (23, 32, 4)
// ExternalWrapper.kt:205 $kotlin.wasm.internal.kotlinToJsStringAdapter (26, 4)
// ExternalWrapper.kt:208 $kotlin.wasm.internal.kotlinToJsStringAdapter
// MemoryAllocation.kt:55 $kotlin.wasm.internal.kotlinToJsStringAdapter
// MemoryAllocation.kt:56 $kotlin.wasm.internal.kotlinToJsStringAdapter (20, 4)
// MemoryAllocation.kt:69 $kotlin.wasm.unsafe.createAllocatorInTheNewScope (20, 38, 20)
// MemoryAllocation.kt:70 $kotlin.wasm.unsafe.createAllocatorInTheNewScope (8, 30, 68, 8)
// MemoryAllocation.kt:88 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init> (4, 4)
// MemoryAllocation.kt:24 $kotlin.wasm.unsafe.MemoryAllocator.<init>
// MemoryAllocation.kt:86 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init>
// MemoryAllocation.kt:90 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init>
// MemoryAllocation.kt:93 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init>
// MemoryAllocation.kt:96 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init>
// MemoryAllocation.kt:142 $kotlin.wasm.unsafe.ScopedMemoryAllocator.<init>
// MemoryAllocation.kt:71 $kotlin.wasm.unsafe.createAllocatorInTheNewScope (23, 4)
// MemoryAllocation.kt:72 $kotlin.wasm.unsafe.createAllocatorInTheNewScope (11, 4)
// MemoryAllocation.kt:58 $kotlin.wasm.internal.kotlinToJsStringAdapter (8, 14)
// MemoryAllocation.kt:160 $kotlin.wasm.internal.kotlinToJsStringAdapter (828, 739, 758, 784, 771, 803, 758, 739, 749, 749, 749, 820, 8580, 8584, 723, 882, 845, 915, 895, 932, 948, 963, 948, 932, 932, 932, 1251, 1261, 1276, 1291, 1276, 1306, 1220, 1353, 1364, 1379, 1364, 1394, 1332, 1325, 1325)
// _Ranges.kt:1321 $kotlin.ranges.coerceAtMost (15, 22, 15, 54, 4)
// MemoryAllocation.kt:99 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (8, 8)
// PreconditionsWasm.kt:29 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (2303, 2302, 2388, 2387, 2728, 2737, 2728, 2742, 2751, 2742, 2760, 2742)
// PreconditionsWasm.kt:17 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (4, 4, 4)
// PreconditionsWasm.kt:20 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (9, 8, 9, 8, 9, 8)
// MemoryAllocation.kt:100 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (8, 8)
// MemoryAllocation.kt:104 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (20, 8)
// MemoryAllocation.kt:105 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (22, 41, 22, 49, 22, 68, 21, 8)
// Primitives.kt:93 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (50, 58, 50)
// Primitives.kt:1281 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (8, 17, 13, 20)
// MemoryAllocation.kt:106 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (8, 8)
// MemoryAllocation.kt:108 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (16, 28, 12, 47, 12)
// MemoryAllocation.kt:112 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (27, 36, 27, 8)
// MemoryAllocation.kt:114 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (29, 48, 29, 8)
// MemoryAllocation.kt:115 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (12, 32, 12)
// MemoryAllocation.kt:125 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (8, 8)
// Preconditions.kt:144 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (456, 475, 494, 475, 456)
// Preconditions.kt:80 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate
// Preconditions.kt:83 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (4, 10)
// Preconditions.kt:27 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate
// Preconditions.kt:29 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (3, 2)
// MemoryAllocation.kt:127 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (30, 15, 8)
// UInt.kt:105 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate
// UInt.kt:414 $kotlin.wasm.unsafe.ScopedMemoryAllocator.allocate (44, 39, 49)
// UInt.kt:17 $kotlin.<UInt__<init>-impl>
// MemoryAccess.kt:16 $kotlin.wasm.unsafe.<Pointer__<init>-impl>
// MemoryAccess.kt:16 $kotlin.wasm.unsafe.<Pointer__<get-address>-impl>
// UInt.kt:17 $kotlin.<UInt__<get-data>-impl>
// Runtime.kt:32 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (18, 4)
// Runtime.kt:33 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (23, 35, 23, 4)
// Runtime.kt:34 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (19, 4)
// Runtime.kt:35 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11)
// Runtime.kt:36 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8)
// Runtime.kt:37 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8)
// Runtime.kt:38 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// Primitives.kt:43 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20)
// Primitives.kt:1150 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16)
// Runtime.kt:40 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory
// MemoryAllocation.kt:60 $kotlin.wasm.internal.kotlinToJsStringAdapter (8, 18)
// MemoryAllocation.kt:139 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy (8, 20, 8)
// MemoryAllocation.kt:140 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:141 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:61 $kotlin.wasm.internal.kotlinToJsStringAdapter (27, 37, 8)
// MemoryAllocation.kt:57 $kotlin.wasm.internal.kotlinToJsStringAdapter
