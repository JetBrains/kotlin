// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = ""
}

suspend fun cleanup() {}

suspend fun concat(x: String, y: String): String = x + y

suspend fun throws() {
    try {
        throw Exception()
    }
    finally {
        cleanup()
    }
}

suspend fun first(x: String, y: String): String = x

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {

    return builder {

        result = "" + concat(
            try {
                ""
            } finally {
                "0"
            },
            "" + concat(
                first(
                    try {
                        try {
                            "O"
                        } finally {
                            "1"
                        }
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        cleanup()
                    },
                    "2"
                ),
                first(
                    try {
                        throws()
                        throw Exception()
                        "3"
                    } catch (e: Exception) {
                        "K"
                    } finally {
                        cleanup()
                    },
                    "4"
                )
            )
        )
    }
}
