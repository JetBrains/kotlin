// FIR_IDENTICAL
typealias ReadonlyArray<T> = Array<T>

fun printAll(vararg values: Any?) {}

fun main() {
    var a: ReadonlyArray<String> = arrayOf("a")
    printAll(values = a)
}