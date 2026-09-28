// MULTILINE_VALUE_PARAMETER_LISTS
fun none() {}

fun single(a: Int) {}

fun several(a: Int, b: String, c: Boolean = true) {}

class Owner(val a: Int, b: String) {
    constructor(a: Int) : this(a, "")

    fun member(x: Long, y: Long) {}

    var counted: Int = 0
        set(value) {
            field = value
        }
}
