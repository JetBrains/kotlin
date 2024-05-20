// java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
//	   at org.jetbrains.kotlin.codegen.optimization.MethodVerifier.transform(MethodVerifier.kt:30)
// IGNORE_BACKEND: JVM

// WITH_STDLIB
// FILECHECK_STAGE: CStubs
const val MaxB = Byte.MAX_VALUE
const val MaxS = Short.MAX_VALUE
const val MaxL = Long.MAX_VALUE
const val MaxC = Char.MAX_VALUE

// CHECK-LABEL: define i32 @"kfun:#testIntSumOf(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testIntSumOf(): Int {
    return (0..10).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntSumOf(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testUIntSumOf(): Int {
    return (0u .. 10u).sumOf { it }.toInt()
}

// CHECK-LABEL: define i32 @"kfun:#testByteSumOf(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue
fun testByteSumOf(): Int {
    return (MaxB .. MaxB).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testShortSumOf(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testShortSumOf(): Int {
    return (MaxS .. MaxS).sumOf { it }
}

// CHECK-LABEL: define i64 @"kfun:#testLongSumOf(){}kotlin.Long
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testLongSumOf(): Long {
    return (MaxL .. MaxL).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntRangeForEach(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testIntRangeForEach(): Int {
    var s = 0
    (1 .. 5).forEach {
        s = s * 10 + it
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntRangeForEach(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testUIntRangeForEach(): Int {
    var s = 0
    (1u .. 5u).forEach {
        s = s * 10 + it.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForEachIndexed(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testIntForEachIndexed(): Int {
    var s = 0
    (1 .. 5).forEachIndexed { index, elem ->
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntForEachIndexed(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testUIntForEachIndexed(): Int {
    var s = 0
    (1u .. 5u).forEachIndexed { index, elem ->
        s = s * 10 + index * elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntIndexOfFirst(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testIntIndexOfFirst(): Int {
    return (1 .. 5).indexOfFirst { it == 3 }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntIndexOfFirst(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testUIntIndexOfFirst(): Int {
    return (1u .. 5u).indexOfFirst { it == 3u }
}

// CHECK-LABEL: define i32 @"kfun:#testIntFirst(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testIntFirst(): Int {
    return (1 .. 5).first { it == 3 }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntFirst(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK-LABEL: epilogue:
fun testUIntFirst(): Int {
    return (1u .. 5u).first { it == 3u }.toInt()
}

// CHECK-LABEL: define i32 @"kfun:#testCharStepReversed(){}kotlin.Int
// CHECK-NOT: Iterable#iterator
// CHECK: kfun:kotlin.internal#getProgressionLastElement
// CHECK-LABEL: epilogue:
fun testCharStepReversed(): Int {
    var s = 0
    val charProgression = 'a'..<'i'
    for (i in (charProgression step 2).reversed()) {
        s += i.code
    }
    return s
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    val intSumOf = testIntSumOf()
    if (intSumOf != 55)
        return "FAIL testIntSumOf: $intSumOf"

    val uintSumOf = testUIntSumOf()
    if (uintSumOf != 55)
        return "FAIL testUIntSumOf: $uintSumOf"

    val byteSumOf = testByteSumOf()
    if (byteSumOf != MaxB.toInt())
        return "FAIL testByteSumOf: $byteSumOf"

    val shortSumOf = testShortSumOf()
    if (shortSumOf != MaxS.toInt())
        return "FAIL testShortSumOf: $shortSumOf"

    val longSumOf = testLongSumOf()
    if (longSumOf != MaxL)
        return "FAIL testLongSumOf: $longSumOf"

    val intForEach = testIntRangeForEach()
    if (intForEach != 12345)
        return "FAIL testIntRangeForEach: $intForEach"

    val uintForEach = testUIntRangeForEach()
    if (uintForEach != 12345)
        return "FAIL testUIntRangeForEach: $uintForEach"

    val intForEachIndexed = testIntForEachIndexed()
    if (intForEachIndexed != 2740)
        return "FAIL testIntForEachIndexed: $intForEachIndexed"

    val uintForEachIndexed = testUIntForEachIndexed()
    if (uintForEachIndexed != 2740)
        return "FAIL testUIntForEachIndexed: $uintForEachIndexed"

    val intIndexOfFirst = testIntIndexOfFirst()
    if (intIndexOfFirst != 2)
        return "FAIL testIntIndexOfFirst: $intIndexOfFirst"

    val uintIndexOfFirst = testUIntIndexOfFirst()
    if (uintIndexOfFirst != 2)
        return "FAIL testUIntIndexOfFirst: $uintIndexOfFirst"

    val intFirstOrNull = testIntFirst()
    if (intFirstOrNull != 3)
        return "FAIL testIntFirstOrNull: $intFirstOrNull"

    val uintFirstOrNull = testUIntFirst()
    if (uintFirstOrNull != 3)
        return "FAIL testUIntFirstOrNull: $uintFirstOrNull"

    val charStepReversed = testCharStepReversed()
    if (charStepReversed != 400)
        return "FAIL testCharStepReversed: $charStepReversed"

    return "OK"
}
