// "Suppress 'REDUNDANT_NULLABLE' for statement " "true"

fun foo() {
    var v = Box<String?>()
    @suppress("REDUNDANT_NULLABLE")
    ++(v: Box<String?<caret>?>)
}

class Box<T> {
    fun inc() = this
}