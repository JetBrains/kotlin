// RUN_PIPELINE_TILL: BACKEND
val f: (String.() -> String)? = null

fun box(): String {
    val g = when {
        f != null -> f
        else -> {
            { this + "K" }
        }
    }
    return g("O")
}