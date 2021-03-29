// "Add 'operator' modifier" "true"
class A {
    fun get(i: Int): String = ""
}

fun foo() = A()<caret>[0]

/* IGNORE_FIR */