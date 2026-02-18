// IGNORE_BACKEND: JVM_IR, JVM_IR_SERIALIZE
// ^^^ Local inline functions are not yet supported.
// FILE: lib.kt
package foo
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: capturedInLambda except=Unit_getInstance
// CHECK_CONTAINS_NO_CALLS: declaredInLambda except=Unit_getInstance

internal data class State(var count: Int = 0)

internal inline fun repeatAction(times: Int, action: () -> Unit) {
    for (i in 1..times) {
        action()
    }
}

// FILE: main.kt
package foo
import kotlin.test.*

// CHECK_CONTAINS_NO_CALLS: capturedInLambda except=Unit_getInstance
// CHECK_CONTAINS_NO_CALLS: declaredInLambda except=Unit_getInstance
// CHECK_BREAKS_COUNT: function=capturedInLambda count=0
// CHECK_LABELS_COUNT: function=capturedInLambda name=$l$block count=0
internal fun capturedInLambda(state: State, a: Int, b: Int): Int {
    inline fun State.inc() {
        count++
    }

    repeatAction(a + b)  {
        state.inc()
    }

    return state.count
}


// CHECK_BREAKS_COUNT: function=declaredInLambda count=0
// CHECK_LABELS_COUNT: function=declaredInLambda name=$l$block count=0
internal fun declaredInLambda(state: State, a: Int, b: Int): Int {
    repeatAction(a)  {
        inline fun State.inc() {
            count++
        }

        repeatAction(b) {
            state.inc()
        }
    }

    return state.count
}


fun box(): String {
    assertEquals(3, capturedInLambda(State(), 1, 2), "capturedInLambda")
    assertEquals(9, capturedInLambda(State(), 4, 5), "capturedInLambda")

    assertEquals(2, declaredInLambda(State(), 1, 2), "declaredInLambda")
    assertEquals(20, declaredInLambda(State(), 4, 5), "declaredInLambda")

    return "OK"
}