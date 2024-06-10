// java.lang.ClassCastException: kotlin.UInt cannot be cast to java.lang.Number
// IGNORE_BACKEND: JVM

// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testUIntForWithIndex(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntForWithIndex(): Int {
    var s = 0
    for ((index, elem) in (1u..5u).withIndex()) {
        s = s * 10 + index * elem.toInt()
    }
    return s
}


// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(2740, testUIntForWithIndex())
    return "OK"
}
