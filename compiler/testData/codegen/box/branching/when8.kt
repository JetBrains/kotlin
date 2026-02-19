// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    when (true) {
        true -> return "OK"
        false -> return "FAIL"
    }
}
