class X {
    fun foo(i: Int) = ""
    fun bar() = ""
    val prop: String = ""
}

fun X.ext1(s: String) = ""
fun X.ext2() = ""
fun X.extProp: String get() = ""

val s: String = X() <caret>

// EXIST: foo
// ABSENT: bar
// ABSENT: prop
// EXIST: ext1
// ABSENT: ext2
// ABSENT: extProp
