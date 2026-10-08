// KT-68395: [K/JS] kotlin.ConcurrentModificationException is not thrown
// REASON: FAIL testMutableListCME(): kotlin.ConcurrentModificationException should have been thrown. sb=OKc
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#testMutableListCME(){}kotlin.String
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#testMutableListCME(){}kotlin.String
// Lists must not be handled by ForLoopsLowering, since its possible modification-in-loop must throw ConcurrentModificationException from its `iterator.next()`
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testMutableListCME(): String {
    val xs = mutableListOf("OK", "b", "c", "d")
    val sb = StringBuilder()
    try {
        for (elem in xs) {
            sb.append(elem)
            xs.remove(elem)
        }
    } catch (e: kotlin.ConcurrentModificationException) {
        return sb.toString()
    }
    return "FAIL testMutableListCME(): kotlin.ConcurrentModificationException should have been thrown. sb=$sb"
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
fun box() = testMutableListCME()
