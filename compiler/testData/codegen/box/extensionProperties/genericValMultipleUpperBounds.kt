// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

import java.io.Serializable

val <T> T.valProp: T where T : Number, T : Serializable
    get() = this

fun box(): String {
    0.valProp

    return "OK"
}
