// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: NONE_APPLICABLE
// !MESSAGE_TYPE: HTML

class A(a: Int) {
}

fun A(a: String) {}

fun main(args: Array<String>) {
    A()
}