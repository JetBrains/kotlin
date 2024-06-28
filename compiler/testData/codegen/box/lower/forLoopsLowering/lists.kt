// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfList(){}kotlin.Int
// Lists must not be handled by ForLoopsLowering, since its modification-in-loop must throw ConcurrentModificationException from its `iterator.next()`
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfList(): Int {
    return listOf(1, 2, 3).sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfArrayList(){}kotlin.Int
// Lists must not be handled by ForLoopsLowering, since its modification-in-loop must throw ConcurrentModificationException from its `iterator.next()`
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfArrayList(): Int {
    return arrayListOf(1, 2, 3).sumOf { it }
}

// CHECK-LABEL: define double @"kfun:#testDoubleSumOfList(){}kotlin.Double
// Lists must not be handled by ForLoopsLowering, since its modification-in-loop must throw ConcurrentModificationException from its `iterator.next()`
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testDoubleSumOfList(): Double {
    return listOf(1.0, 2.0, 3.0).sumOf { it }
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(6, testIntSumOfList())
    assertEquals(6, testIntSumOfArrayList())
    assertEquals(6.0, testDoubleSumOfList())
    return "OK"
}
