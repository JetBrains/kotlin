// !DIAGNOSTICS: -UNUSED_PARAMETER

fun printAll(vararg a : Any) {}

fun main(args: Array<String>) {
    printAll(*args) // Shouldn't be an error
}