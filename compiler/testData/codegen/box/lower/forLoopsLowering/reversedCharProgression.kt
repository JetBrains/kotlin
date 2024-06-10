// java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
//     at org.jetbrains.kotlin.codegen.optimization.MethodVerifier.transform(MethodVerifier.kt:30)
// IGNORE_BACKEND: JVM

// WITH_STDLIB
// FILECHECK_STAGE: CStubs
import kotlin.test.*

// CHECK-LABEL: define i32 @"kfun:#testReversedCharProgression(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testReversedCharProgression(): Int {
    var s = 0
    val charProgression = 'a'..<'i'
    val value = charProgression step 2
    val reversed = value.reversed()
    for (i in reversed) {
        s += i.code
    }
    return s
}

// CHECK-LABEL: define i32 @"kfun:#testForEachReversedCharProgression(){}kotlin.Int
// CHECK-NOT: iterator
// CHECK-LABEL: epilogue:
fun testForEachReversedCharProgression(): Int {
    var s = 0
    val charProgression = 'a'..<'i'
    val value = charProgression step 2
    val reversed = value.reversed()
    reversed.forEach { s += it.code }
    return s
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    assertEquals(400, testReversedCharProgression())
    assertEquals(400, testForEachReversedCharProgression())
    return "OK"
}
