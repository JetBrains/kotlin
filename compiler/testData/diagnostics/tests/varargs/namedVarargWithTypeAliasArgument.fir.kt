typealias ReadonlyArray<T> = Array<T>

fun printAll(vararg values: Any?) {}

fun main() {
    var a: ReadonlyArray<String> = arrayOf("a")
    printAll(values = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>a<!>)
}