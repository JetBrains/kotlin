// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfReversedArray(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfReversedArray(): Int {
    val intArrayOf = intArrayOf(1, 2, 3)
    val reversedArray = intArrayOf.reversedArray()
    return reversedArray.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfIntRangeReversed(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfIntRangeReversed(): Int {
    val intRange = 1..3
    val reversed = intRange.reversed()
    return reversed.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfArrayReversed(){}kotlin.Int
// `reversed` of an array is not yet supported
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfArrayReversed(): Int {
    return intArrayOf(1, 2, 3).reversed().sumOf { it }
}


// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexReversed(){}kotlin.Int
// KT-68357: `kotlin.collections.reversed(): List` is not yet supported by ReversedHandler and ProgressionType.fromIrType()
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexReversed(): Int {
    var s = 0
    for ((index, elem) in (1..5).withIndex().reversed()) {
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(6, testIntSumOfReversedArray())
    assertEquals(6, testIntSumOfIntRangeReversed())
    assertEquals(6, testIntSumOfArrayReversed())
    assertEquals(212620, testIntForWithIndexReversed())
    return "OK"
}
