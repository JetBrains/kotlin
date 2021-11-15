// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class TodoItem(var value: String, var completed: Boolean) {
    override fun toString(): String {
        return "TodoItem(value='$value', completed=$completed)"
    }
}

suspend fun getFromApi(): TodoItem {
    return TodoItem("Test", false)
}

fun emulateLog(vararg strings: String): String {
    return strings[0]
}

fun box(): String {
    var stringifiedResult = ""

    builder {
        stringifiedResult = emulateLog("Result: " + getFromApi())
    }

    if (stringifiedResult != "Result: TodoItem(value='Test', completed=false)") {
        return "Failed: Unexpected result ($stringifiedResult)"
    }
    return "OK"
}