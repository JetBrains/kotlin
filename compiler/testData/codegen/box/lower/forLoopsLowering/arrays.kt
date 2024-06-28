// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfArray(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfArray(): Int {
    return intArrayOf(1, 2, 3).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfArrayVal(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfArrayVal(): Int {
    val intArrayOf = intArrayOf(1, 2, 3)
    return intArrayOf.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testUIntSumOfArray(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntSumOfArray(): Int {
    val uintArrayOf = uintArrayOf(1u, 2u, 3u)
    return uintArrayOf.sumOf { it.toInt() }
}

// CHECK-LABEL: define i32 @"kfun:#testShortSumOfArray(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testShortSumOfArray(): Int {
    val shortArrayOf = shortArrayOf(1, 2, 3)
    return shortArrayOf.sumOf { it.toInt() }
}

// CHECK-LABEL: define i32 @"kfun:#testUShortSumOfArray(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUShortSumOfArray(): Int {
    val ushortArrayOf = ushortArrayOf(1u, 2u, 3u)
    return ushortArrayOf.sumOf { it.toInt() }
}

// CHECK-LABEL: define float @"kfun:#testFloatSumOfArray(){}kotlin.Float
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testFloatSumOfArray(): Float {
    var sum = 0F
    for (elem in floatArrayOf(1F, 5F)) {
        sum += elem
    }
    return sum
}


// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(6, testIntSumOfArray())
    assertEquals(6, testIntSumOfArrayVal())
    assertEquals(6, testUIntSumOfArray())
    assertEquals(6, testShortSumOfArray())
    assertEquals(6, testUShortSumOfArray())
    assertEquals(6F, testFloatSumOfArray())
    return "OK"
}
