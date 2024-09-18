// TARGET_BACKEND: JS

data class Some(val a: String, val b: dynamic)

fun box(): String {
    val event = Some("O", "K")
    event.hashCode()
    return "OK"
}