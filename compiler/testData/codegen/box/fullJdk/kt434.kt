// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// FULL_JDK

import java.net.*

fun String.decodeURI(encoding : String) : String? =
    try {
        URLDecoder.decode(this, encoding)
    }
    catch (e : Throwable) {
        null
    }

fun box() : String {
    return if("hhh".decodeURI("") == null) "OK" else "fail"
}
