// WITH_RUNTIME

abstract class Abs()

fun foo() {
    val v = object : Abs() {
        val a: String

        fun foo() {
            <!MAY_BE_NOT_INITIALIZED!>a<!>.length
        }

        init {
            <!LEAKING_THIS!>foo()<!>
            a = ""
        }
    }

    print(v)
}
