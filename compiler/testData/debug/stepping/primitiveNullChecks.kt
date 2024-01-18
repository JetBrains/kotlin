
// FILE: test.kt

fun box(): String {
    42!!
    42.toLong()!!
    return "OK"!!
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:6 box
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:1 $box__JsExportAdapter
// test.kt:5 $box
// test.kt:6 $box
// test.kt:7 $box (11, 11, 11, 11, 4)
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
// Runtime.kt:35 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (11, 22, 11, 11, 11, 11, 22, 11, 11, 11, 11, 22, 11, 11, 11)
// Runtime.kt:36 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (25, 34, 42, 38, 8, 25, 34, 42, 38, 8)
// Runtime.kt:37 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 19, 8, 8, 8, 19, 8, 8)
// Runtime.kt:38 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 8, 8, 8)
// Primitives.kt:43 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (20, 20)
// Primitives.kt:1150 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 15, 8, 16, 8, 15, 8, 16)
// Runtime.kt:40 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory
// ExternalWrapper.kt:226 $kotlin.wasm.internal.jsCheckIsNullOrUndefinedAdapter (18, 8, 32, 33)
// MemoryAllocation.kt:60 $kotlin.wasm.internal.kotlinToJsStringAdapter (8, 18)
// MemoryAllocation.kt:139 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy (8, 20, 8)
// MemoryAllocation.kt:140 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:141 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:61 $kotlin.wasm.internal.kotlinToJsStringAdapter (27, 37, 8)
// MemoryAllocation.kt:57 $kotlin.wasm.internal.kotlinToJsStringAdapter
