
// FILE: test.kt

fun foo() {
    try {
        mightThrow()
    } finally {
        "FINALLY"
    }
    
    val t = try {
        mightThrow2()
    } finally {
        "FINALLY2"
    }
}

var throw1 = false
var throw2 = false

fun mightThrow() {
    if (throw1) throw Exception()
}

fun mightThrow2() {
    if (throw2) throw Exception()
}

fun box() {
    foo()
    throw2 = true
    foo()
    // Never gets here.
    throw1 = true
    foo()
}

// EXPECTATIONS JVM_IR
// test.kt:30 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:22 mightThrow
// test.kt:23 mightThrow
// test.kt:8 foo
// test.kt:9 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:26 mightThrow2
// test.kt:27 mightThrow2
// test.kt:14 foo
// test.kt:15 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:31 box
// test.kt:32 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:22 mightThrow
// test.kt:23 mightThrow
// test.kt:8 foo
// test.kt:9 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:26 mightThrow2
// test.kt:14 foo

// EXPECTATIONS JS_IR
// test.kt:30 box
// test.kt:6 foo
// test.kt:22 mightThrow
// test.kt:23 mightThrow
// test.kt:12 foo
// test.kt:26 mightThrow2
// test.kt:27 mightThrow2
// test.kt:11 foo
// test.kt:16 foo
// test.kt:31 box
// test.kt:32 box
// test.kt:6 foo
// test.kt:22 mightThrow
// test.kt:23 mightThrow
// test.kt:12 foo
// test.kt:26 mightThrow2
// test.kt:26 mightThrow2

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:30 $box
// test.kt:6 $foo (8, 8)
// test.kt:22 $mightThrow (8, 8)
// test.kt:23 $mightThrow (1, 1)
// test.kt:5 $foo (4, 4)
// test.kt:8 $foo (8, 8, 8, 8, 8, 8, 8, 8)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4, 11, 4)
// test.kt:12 $foo (8, 8)
// test.kt:26 $mightThrow2 (8, 8, 22, 22, 16)
// test.kt:27 $mightThrow2
// test.kt:11 $foo (12, 4, 12)
// test.kt:14 $foo (8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:16 $foo
// test.kt:31 $box (13, 4)
// test.kt:32 $box
// String.kt:143 $kotlin.stringLiteral (15, 8, 15, 8)
// Exceptions.kt:16 $kotlin.Exception.<init> (34, 34, 4, 4, 41)
// Throwable.kt:23 $kotlin.Throwable.<init> (32, 38, 27, 27, 43)
// Throwable.kt:18 $kotlin.Throwable.<init> (28, 62)
// Throwable.kt:25 $kotlin.Throwable.<init>
// ExternalWrapper.kt:226 $kotlin.wasm.internal.jsCheckIsNullOrUndefinedAdapter (18, 8, 32, 33, 18, 8, 32, 33)
// Throwable.kt:27 $kotlin.Throwable.<init>
// Throwable.kt:39 $kotlin.Throwable.<init>
// Throwable.kt:49 $kotlin.Throwable.<init>
// Exceptions.kt:20 $kotlin.Exception.<init>
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
// Runtime.kt:35 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11)
// Runtime.kt:36 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8, 25, 34, 42, 38, 8)
// Runtime.kt:37 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8, 8, 19, 8, 8)
// Runtime.kt:38 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// Primitives.kt:43 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (20, 20, 20, 20, 20, 20, 20, 20, 20)
// Primitives.kt:1150 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16, 8, 15, 8, 16)
// Runtime.kt:40 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory
// MemoryAllocation.kt:60 $kotlin.wasm.internal.kotlinToJsStringAdapter (8, 18)
// MemoryAllocation.kt:139 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy (8, 20, 8)
// MemoryAllocation.kt:140 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:141 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:61 $kotlin.wasm.internal.kotlinToJsStringAdapter (27, 37, 8)
// MemoryAllocation.kt:57 $kotlin.wasm.internal.kotlinToJsStringAdapter
