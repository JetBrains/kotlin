// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A {
    operator fun getValue(x: Any?, y: Any?): Any?
}

interface B : A {
    override fun getValue(x: Any?, y: Any?): Int
}

fun test(a: A) {
    if (a is B) {
        val x: Int by <!DEBUG_INFO_SMARTCAST!>a<!>
    }
}
