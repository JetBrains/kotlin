// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndex(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndex(): Int {
    var s = 0
    for ((index, elem) in (1..5).withIndex()) {
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexNotUsed(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexNotUsed(): Int {
    var s = 0
    for ((_, _) in (1..5).withIndex()) {
        s++
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexPartiallyUsed(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexPartiallyUsed(): Int {
    var s = 0
    for ((index, _) in (1..5).withIndex()) {
        s = s * 10 + index
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexAndDestructor(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexAndDestructor(): Int {
    var s = 0
    for (ie in (1..5).withIndex()) {
        val (index, elem) = ie
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexAndDestructorNotUsed(){}kotlin.Int
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexAndDestructorNotUsed(): Int {
    var s = 0
    for (ie in (1..5).withIndex()) {
        s++
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntArrayForWithIndex(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testIntArrayForWithIndex(): Int {
    var s = 0
    for ((index, elem) in intArrayOf(1, 2, 3, 4, 5).withIndex()) {
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testUIntArrayForWithIndex(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testUIntArrayForWithIndex(): Int {
    var s = 0
    for ((index, elem) in uintArrayOf(1u, 2u, 3u, 4u, 5u).withIndex()) {
        s = s * 10 + index * elem.toInt()
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testForStringWithIndex(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testForStringWithIndex(): Int {
    var s = 0
    for ((index, elem) in "ABC".withIndex()) {
        s = s * 10 + index * elem.code
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForWithIndexWithTemporary(){}kotlin.Int
// KT-68357: After implementation of KT-68357, usage of `iterator` must disappear
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntForWithIndexWithTemporary(): Int {
    var s = 0
    val intRange = 1..5
    val withIndex1 = intRange.withIndex()
    val withIndex2 = withIndex1
    for ((index, elem) in withIndex2) {
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntWithIndexForEach(){}kotlin.Int
// KT-68357: After implementation of KT-68357, usage of `iterator` must disappear
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntWithIndexForEach(): Int {
    var s = 0
    (1 .. 5).withIndex().forEach { (index, elem) ->
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntSequenceWithIndex(){}kotlin.Int
// Sequences are not yet supported
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntSequenceWithIndex(): Int {
    var s = 0
    for ((index, elem) in (0..4).asSequence().withIndex()) {
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testIntForEachSequenceWithIndex(){}kotlin.Int
// Sequences are not yet supported
// CHECK: iterator
// CHECK-LABEL: epilogue:
fun testIntForEachSequenceWithIndex(): Int {
    var s = 0
    (0..4).asSequence().withIndex().forEach { (index, elem) ->
        s = s * 10 + index * elem
    }
    return s
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(2740, testIntForWithIndex())
    assertEquals(2740, testIntForWithIndexAndDestructor())
    assertEquals(1234, testIntForWithIndexPartiallyUsed())
    assertEquals(5, testIntForWithIndexNotUsed())
    assertEquals(5, testIntForWithIndexAndDestructorNotUsed())
    assertEquals(2740, testIntArrayForWithIndex())
    assertEquals(2740, testUIntArrayForWithIndex())
    assertEquals(794, testForStringWithIndex())
    assertEquals(2740, testIntForWithIndexWithTemporary())
    assertEquals(2740, testIntWithIndexForEach())
    assertEquals(1506, testIntSequenceWithIndex())
    assertEquals(1506, testIntForEachSequenceWithIndex())
    return "OK"
}
