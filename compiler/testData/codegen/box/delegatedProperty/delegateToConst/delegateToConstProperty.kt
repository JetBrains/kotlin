// WITH_STDLIB

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}
const val a = "TEXT"

val s: String by a

fun box(): String = s
