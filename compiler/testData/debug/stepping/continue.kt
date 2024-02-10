
// WITH_STDLIB
// FILE: test.kt

val strings = arrayOf("1a", "1b", "2", "3")

fun box() {
    for (s in strings) {
        if (s == "1a" || s == "1b") {
            continue
        }
        if (s == "2") {
            continue
        }
        println(s)
    }
}

// EXPECTATIONS JVM_IR
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:8 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:5 <get-strings>
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:10 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:8 box
// test.kt:8 box
// test.kt:8 box
// test.kt:9 box
// test.kt:12 box
// test.kt:15 box
// test.kt:8 box
// test.kt:17 box

// EXPECTATIONS WASM
// test.kt:5 $<init properties test.kt>
// Library.kt:2 $<init properties test.kt> (53, 53, 53, 53, 59, 59, 59, 59, 65, 65, 65, 65, 70, 70, 70, 70, 53, 53)
// String.kt:141 $kotlin.stringLiteral (17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17, 17, 28, 17)
// Array.kt:59 $kotlin.Array.get (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// ThrowHelpers.kt:29 $kotlin.wasm.internal.rangeCheck (6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19, 6, 14, 6, 19, 28, 19)
// ThrowHelpers.kt:30 $kotlin.wasm.internal.rangeCheck (1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
// Array.kt:60 $kotlin.Array.get (15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8, 15, 27, 23, 8)
// String.kt:142 $kotlin.stringLiteral (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// String.kt:146 $kotlin.stringLiteral (47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4, 47, 61, 16, 4)
// String.kt:147 $kotlin.stringLiteral (20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4, 20, 20, 20, 20, 27, 33, 41, 20, 4)
// String.kt:148 $kotlin.stringLiteral (4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4, 4, 15, 25, 4)
// Array.kt:74 $kotlin.Array.set (19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8, 19, 26, 34, 8)
// Array.kt:75 $kotlin.Array.set (8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16, 8, 20, 27, 16)
// Array.kt:76 $kotlin.Array.set (5, 5, 5, 5)
// String.kt:149 $kotlin.stringLiteral (11, 4, 11, 4, 11, 4, 11, 4)
// Library.kt:39 $<init properties test.kt> (62, 82)
// test.kt:1 $box
// test.kt:8 $box (14, 14, 14, 14, 14, 4, 14, 4, 4, 4, 14, 9, 14, 4, 14, 4, 4, 4, 14, 9, 14, 4, 14, 4, 4, 4, 14, 9, 14, 4, 14, 4, 4, 4, 14, 9, 14, 4, 14, 4, 4, 4)
// test.kt:5 $<get-strings>
// Array.kt:82 $kotlin.Array.<get-size> (16, 24, 29)
// test.kt:9 $box (12, 17, 17, 17, 17, 12, 12, 17, 17, 17, 17, 12, 25, 30, 30, 30, 30, 25, 12, 17, 17, 17, 17, 12, 25, 30, 30, 30, 30, 25, 12, 17, 17, 17, 17, 12, 25, 30, 30, 30, 30, 25)
// String.kt:143 $kotlin.stringLiteral (15, 8, 15, 8, 15, 8, 15, 8, 15, 8, 15, 8, 15, 8, 15, 8, 15, 8)
// String.kt:98 $kotlin.String.equals (12, 12, 12, 12, 12, 12, 12, 12, 12)
// String.kt:99 $kotlin.String.equals (12, 35, 28, 12, 12, 35, 28, 12, 12, 12, 35, 28, 12, 12, 12)
// test.kt:10 $box (12, 12)
// String.kt:100 $kotlin.String.equals (26, 26, 8, 26, 26, 8, 26, 26, 8, 26, 26, 8, 26, 26, 8, 26, 26, 8)
// String.kt:102 $kotlin.String.equals (30, 8, 30, 8, 30, 8, 30, 8, 30, 8, 30, 8)
// String.kt:103 $kotlin.String.equals (26, 38, 8, 26, 38, 8, 26, 38, 8, 26, 38, 8, 26, 38, 8, 26, 38, 8)
// String.kt:104 $kotlin.String.equals (12, 26, 12, 12, 12, 26, 12, 12, 46, 39, 12, 26, 12, 12, 46, 39, 12, 26, 12, 12, 46, 39, 12, 26, 12, 12, 46, 39, 12, 26, 12, 12)
// String.kt:106 $kotlin.String.equals (28, 8, 28, 8)
// String.kt:107 $kotlin.String.equals (24, 30, 8, 24, 30, 8)
// String.kt:108 $kotlin.String.equals (12, 24, 12, 12, 12, 24, 12, 12)
// String.kt:110 $kotlin.String.equals (29, 8, 29, 8)
// String.kt:63 $kotlin.String.equals (12, 12, 12, 12)
// String.kt:66 $kotlin.String.equals (15, 8, 15, 8, 15, 8, 15, 8)
// String.kt:111 $kotlin.String.equals (31, 25, 8, 31, 25, 8)
// String.kt:112 $kotlin.String.equals (8, 8)
// Standard.kt:124 $kotlin.String.equals (2, 32, 46, 42, 53, 32, 32, 32, 46, 42, 53, 32, 32, 2, 32, 46, 42, 53, 32, 32)
// Standard.kt:152 $kotlin.String.equals (4, 4)
// Standard.kt:154 $kotlin.String.equals (18, 4, 26, 4, 18, 9, 18, 4, 26, 4, 4, 18, 9, 18, 18, 4, 26, 4, 18, 9, 18)
// Standard.kt:155 $kotlin.String.equals (8, 15, 8, 15, 8, 15)
// Standard.kt:126 $kotlin.String.equals (1, 1, 13, 6, 1, 13, 6)
// Standard.kt:125 $kotlin.String.equals (3, 3, 3)
// test.kt:12 $box (12, 17, 17, 17, 17, 12, 12, 17, 17, 17, 17, 12)
// test.kt:13 $box
// test.kt:15 $box (16, 8)
// io.kt:28 $kotlin.io.println (16, 16, 25, 25, 25, 4)
// String.kt:119 $kotlin.String.toString
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
// Runtime.kt:35 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (11, 22, 11, 11, 11, 11, 22, 11, 11, 11)
// Runtime.kt:36 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (25, 34, 42, 38, 8)
// Runtime.kt:37 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 19, 8, 8)
// Runtime.kt:38 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 8)
// Primitives.kt:43 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory
// Primitives.kt:1150 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory (8, 15, 8, 16)
// Runtime.kt:40 $kotlin.wasm.internal.unsafeWasmCharArrayToRawMemory
// ExternalWrapper.kt:226 $kotlin.wasm.internal.jsCheckIsNullOrUndefinedAdapter (18, 8, 32, 33)
// MemoryAllocation.kt:60 $kotlin.wasm.internal.kotlinToJsStringAdapter (8, 18)
// MemoryAllocation.kt:139 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy (8, 20, 8)
// MemoryAllocation.kt:140 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:141 $kotlin.wasm.unsafe.ScopedMemoryAllocator.destroy
// MemoryAllocation.kt:61 $kotlin.wasm.internal.kotlinToJsStringAdapter (27, 37, 8)
// MemoryAllocation.kt:57 $kotlin.wasm.internal.kotlinToJsStringAdapter
// io.kt:29 $kotlin.io.println
// test.kt:17 $box
