// WITH_STDLIB
fun function() = 1u

var variable = 1L

val getter get() = "Hello"

var initializerAndAccessors = 'c'
    get() = field
    set(value) {
        field = value
    }

fun container() {
    val local = 1.0
    fun localFunction() = 1 < 2
}
