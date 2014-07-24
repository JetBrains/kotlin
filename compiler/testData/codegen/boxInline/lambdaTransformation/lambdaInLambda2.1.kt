import test.*
import java.util.*

fun test1(prefix: String): String {
    var result = "fail"
    mfun {
        concat("start") {
            if (it.startsWith(prefix)) {
                result = "OK"
            }
        }
    }

    return result
}

fun box(): String {
    if (test1("start") != "OK") return "fail1"
    if (test1("nostart") != "fail") return "fail2"

    return "OK"
}