fun Int.map(<!UNUSED_PARAMETER!>f<!>: (Int) -> Int): List<Int> = throw AssertionError("")
fun Int.map(<!UNUSED_PARAMETER!>f<!>: (Int) -> Int, <!UNUSED_PARAMETER!>b<!>: Boolean = true): List<Int> = throw AssertionError("")

fun foo(): List<Int> = for (i in 10) yield i*i