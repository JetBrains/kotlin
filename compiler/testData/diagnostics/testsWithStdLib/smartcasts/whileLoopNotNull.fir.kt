// FIR_DUMP

fun Int?.swap(): Int = 1
fun Int.swap(): Int? = null

var result = false

fun b(): Boolean {
    result = !result
    return result
}

fun test() {
    var x: Int? = 1
    if (x != null) {
        while (b()) {
            val tmp = x.swap()
            x = tmp
        }
        x.plus(1)
    }
}
