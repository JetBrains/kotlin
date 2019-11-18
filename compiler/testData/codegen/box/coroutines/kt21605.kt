// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

interface Consumer { fun consume(s: String) }

inline fun crossInlineBuilder(crossinline block: (String) -> Unit) = object : Consumer {
    override fun consume(s: String) {
        block(s)
    }
}

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
