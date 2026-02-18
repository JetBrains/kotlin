// WITH_STDLIB
// WITH_COROUTINES
// FILE: lib.kt
import kotlin.coroutines.*

interface Consumer { fun consume(s: String) }

inline fun crossInlineBuilder(crossinline block: (String) -> Unit) = object : Consumer {
    override fun consume(s: String) {
        block(s)
    }
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*

fun builder(block: suspend Unit.() -> Unit) {
    block.startCoroutine(Unit, EmptyContinuation)
}

class Container {
    var y: String = "FAIL"

    val consumer = crossInlineBuilder { s ->
        builder {
            y = s
        }
    }
}

fun box(): String {
    val container = Container()
    container.consumer.consume("OK")
    return container.y
}
