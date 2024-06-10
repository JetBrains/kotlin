// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#changeRangeToListByFun(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun changeRangeToListByFun(): Int {
    var sum = 0
    var range: Iterable<Int> = (0..10)
    fun doTheChange() {
        range = listOf(1, 2, 3)
    }
    doTheChange()
    for (x in range) {
        sum += x
    }
    return sum
}

// CHECK-LABEL: define i32 @"kfun:#changeRangeToListExplicit(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun changeRangeToListExplicit(): Int {
    var sum = 0
    var range: Iterable<Int> = (0..10)
    range = listOf(1, 2, 3, 4)
    for (x in range) {
        sum += x
    }
    return sum
}

// CHECK-LABEL: define i32 @"kfun:#changeGlobalRangeToList(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
var globalRange: Iterable<Int> = (0..10)
fun changeGlobalRangeToList(): Int {
    var sum = 0
    globalRange = listOf(1, 2, 3, 4, 5)
    for (x in globalRange) {
        sum += x
    }
    return sum
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(6, changeRangeToListByFun())
    assertEquals(10, changeRangeToListExplicit())
    assertEquals(15, changeGlobalRangeToList())
    return "OK"
}
