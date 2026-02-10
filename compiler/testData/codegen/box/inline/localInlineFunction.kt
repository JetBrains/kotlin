// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// ^^^ Local inline functions are not yet supported.
// FILE: lib.kt
package foo
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: localWithCapture except=Unit_getInstance
// CHECK_CONTAINS_NO_CALLS: localWithoutCapture

internal inline fun repeatAction(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: localWithCapture except=Unit_getInstance
// CHECK_CONTAINS_NO_CALLS: localWithoutCapture
// CHECK_BREAKS_COUNT: function=localWithoutCapture count=0
// CHECK_LABELS_COUNT: function=localWithoutCapture name=$l$block count=0
internal fun localWithoutCapture(a: Int, b: Int): Int {
    var sum = 0

    inline fun inc(x: Int): Int {
        return x + 1
    }

    repeatAction(a + b)  {
        sum = inc(sum)
    }

    return sum
}

// CHECK_BREAKS_COUNT: function=localWithCapture count=0
// CHECK_LABELS_COUNT: function=localWithCapture name=$l$block count=0
internal fun localWithCapture(a: Int, b: Int): Int {
    var sum = 0

    inline fun inc() {
        sum++
    }

    repeatAction(a + b)  {
        inc()
    }

    return sum
}

fun box(): String {
    assertEquals(3, localWithoutCapture(1, 2), "localWithoutCapture")
    assertEquals(9, localWithoutCapture(4, 5), "localWithoutCapture")

    assertEquals(3, localWithCapture(1, 2), "localWithCapture")
    assertEquals(9, localWithCapture(4, 5), "localWithCapture")

    return "OK"
}