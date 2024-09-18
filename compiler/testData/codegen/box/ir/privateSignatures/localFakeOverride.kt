// TARGET_BACKEND: JS

// MODULE: lib
// FILE: l.kt

var result = ""

fun foo(b: Boolean) {

    class P1

    abstract class A1 {
        fun bar(p1: P1): String = "AB"
        abstract fun qux(): String
    }
    if (b) {

        class C1 : A1() {
            override fun qux(): String = "C1T"
        }

        val c1 = C1()
        result += c1.qux()
        result += c1.bar(P1())
    } else {
        class C1 : A1() {
            override fun qux(): String = "C1F"
        }

        val c1 = C1()
        result += c1.qux()
        result += c1.bar(P1())
    }

}

fun r(): String = result


// MODULE: main(lib)
// FILE: m.kt


fun box(): String {
    foo(true)
    foo(false)

    val rr = r()
    if (rr != "C1TABC1FAB") return "FAIL: $rr"

    return "OK"
}