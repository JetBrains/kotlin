// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// ^^^ Local inline functions are not yet supported.
// FILE: lib.kt
package foo
import kotlin.test.*

internal inline fun run(action: () -> Int): Int {
    return action()
}

// FILE: main.kt
package foo
import kotlin.test.*
// CHECK_CONTAINS_NO_CALLS: myAdd
// CHECK_BREAKS_COUNT: function=myAdd count=0
// CHECK_LABELS_COUNT: function=myAdd name=$l$block count=0
internal fun myAdd(a: Int, b: Int): Int {
    var sum = a + b

    inline fun getSum(): Int {
        return sum
    }

    return run {
        var sum = 0

        run {
            sum = -1
            getSum()
        }
    }
}

fun box(): String {
    assertEquals(3, myAdd(1, 2))

    return "OK"
}