// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
