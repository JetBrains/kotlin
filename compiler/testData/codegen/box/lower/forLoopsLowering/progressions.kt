// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

const val MaxB = Byte.MAX_VALUE
const val MaxS = Short.MAX_VALUE
const val MaxL = Long.MAX_VALUE
const val MaxC = Char.MAX_VALUE

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableInt(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableInt(): Int {
    val intRange: Iterable<Int> = 0..10
    return intRange.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIterableIntTemporary(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIterableIntTemporary(): Int {
    return (0..10).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOf(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOf(): Int {
    val intRange = 0..10
    return intRange.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntSumOf(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntSumOf(): Int {
    val uintRange = 0u..10u
    return uintRange.sumOf { it }.toInt()
}

// CHECK-LABEL: define i32 @"kfun:#testByteSumOf(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue
fun testByteSumOf(): Int {
    val byteRange = MaxB..MaxB
    return byteRange.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testShortSumOf(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testShortSumOf(): Int {
    val shortRange = MaxS..MaxS
    return shortRange.sumOf { it }
}

// CHECK-LABEL: define i64 @"kfun:#testLongSumOf(){}kotlin.Long
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testLongSumOf(): Long {
    val longRange = MaxL..MaxL
    return longRange.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfDownTo(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfDownTo(): Int {
    val downTo = 10.downTo(0)
    return downTo.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntRange(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntRange(): Int {
    var s = 0
    val intRange = 1..5
    for (elem in intRange) {
        s = s * 10 + elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntRange(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntRange(): Int {
    var s = 0
    val uintRange = 1u..5u
    for (elem in uintRange) {
        s = s * 10 + elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testLongRange(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testLongRange(): Int {
    var s = 0
    val longRange = 1L..5L
    for (elem in longRange) {
        s = s * 10 + elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testULongRange(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testULongRange(): Int {
    var s = 0
    val ulongRange = 1uL..5uL
    for (elem in ulongRange) {
        s = s * 10 + elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntRangeForEach(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntRangeForEach(): Int {
    var s = 0
    val intRange = 1..5
    intRange.forEach {
        s = s * 10 + it
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntRangeForEach(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntRangeForEach(): Int {
    var s = 0
    val uintRange = 1u..5u
    uintRange.forEach {
        s = s * 10 + it.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForEachIndexed(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntForEachIndexed(): Int {
    var s = 0
    val intRange = 1..5
    intRange.forEachIndexed { index, elem ->
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntForEachIndexed(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntForEachIndexed(): Int {
    var s = 0
    val uintRange = 1u..5u
    uintRange.forEachIndexed { index, elem ->
        s = s * 10 + index * elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntIndexOfFirst(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntIndexOfFirst(): Int {
    val intRange = 1..5
    return intRange.indexOfFirst { it == 3 }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntIndexOfFirst(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntIndexOfFirst(): Int {
    val uintRange = 1u..5u
    return uintRange.indexOfFirst { it == 3u }
}

// CHECK-LABEL: define i32 @"kfun:#testIntFirst(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntFirst(): Int {
    val intRange = 1..5
    return intRange.first { it == 3 }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntFirst(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntFirst(): Int {
    val uintRange = 1u..5u
    return uintRange.first { it == 3u }.toInt()
}

// CHECK-LABEL: define i32 @"kfun:#testCharProgression(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testCharProgression(): Int {
    var sum = 0
    val str = "ABC"
    for (char in str) sum += char.code
    return sum
}

// CHECK-LABEL: define i32 @"kfun:#testSumOfCharProgression(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testSumOfCharProgression(): Int {
    val str = "ABC"
    return str.sumOf { it.code }
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(55, testIntSumOfIterableInt())
    assertEquals(55, testIntSumOfIterableIntTemporary())
    assertEquals(55, testIntSumOf())
    assertEquals(55, testUIntSumOf())
    assertEquals(MaxB.toInt(), testByteSumOf())
    assertEquals(MaxS.toInt(), testShortSumOf())
    assertEquals(MaxL, testLongSumOf())
    assertEquals(55, testIntSumOfDownTo())
    assertEquals(12345, testIntRange())
    assertEquals(12345, testUIntRange())
    assertEquals(12345, testLongRange())
    assertEquals(12345, testULongRange())
    assertEquals(12345, testIntRangeForEach())
    assertEquals(12345, testUIntRangeForEach())
    assertEquals(2740, testIntForEachIndexed())
    assertEquals(2740, testUIntForEachIndexed())
    assertEquals(2, testIntIndexOfFirst())
    assertEquals(2, testUIntIndexOfFirst())
    assertEquals(3, testIntFirst())
    assertEquals(3, testUIntFirst())
    assertEquals(198, testCharProgression())
    assertEquals(198, testSumOfCharProgression())
    return "OK"
}
