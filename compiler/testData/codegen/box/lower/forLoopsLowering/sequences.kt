// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfSequence(){}kotlin.Int
// Sequences must not be handled by ForLoopsLowering, since iterator is the only way to iterate sequences
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfSequence(): Int {
    val sequence = Sequence {
        val intRange = 0..10
        intRange.iterator()
    }
    return sequence.sumOf { it }
}

// CHECK-LABEL: define i32 @"kfun:#testIntSumOfAsSequence(){}kotlin.Int
// Sequences must not be handled by ForLoopsLowering, since iterator is the only way to iterate sequences
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSumOfAsSequence(): Int {
    val intRange = 0..10
    val asSequence = intRange.asSequence()
    return asSequence.sumOf { it }
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(55, testIntSumOfSequence())
    assertEquals(55, testIntSumOfAsSequence())
    return "OK"
}
