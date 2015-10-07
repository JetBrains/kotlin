// "Suppress 'REDUNDANT_NULLABLE' for statement " "true"

fun foo() {
    var v = Box<String?>()
    (v as Box<String?<caret>?>)++
}

class Box<T> {
    fun inc() = this
}