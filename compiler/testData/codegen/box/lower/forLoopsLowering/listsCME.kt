// KT-68395: [K/JS] RangeError is thrown instead of kotlin.ConcurrentModificationException
// RangeError: Invalid array length
//    at Array.push (<anonymous>)
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#testMutableListCME(){}kotlin.String
// Lists must not be handled by ForLoopsLowering, since its possible modification-in-loop must throw ConcurrentModificationException from its `iterator.next()`
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testMutableListCME(): String {
    val xs = mutableListOf("a", "b", "c", "d")
    val sb = StringBuilder()
    var cmeThrown = false
    try {
        for (elem in xs) {
            sb.append(elem)
            xs.add(elem)
        }
    } catch (e: kotlin.ConcurrentModificationException) {
        cmeThrown = true
    }
    if (!cmeThrown) return "FAIL testMutableListCME(): kotlin.ConcurrentModificationException should have been thrown. sb=$sb"
    return sb.toString()
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals("a", testMutableListCME())
    return "OK"
}
