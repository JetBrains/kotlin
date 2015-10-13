class X {
    infix fun foo(i: Int) = ""
    fun bar(i: Int) = ""
    val prop: String = ""
}

infix fun X.ext1(s: String) = ""
fun X.ext2(p: Int) = ""
fun X.extProp: String get() = ""

val s: String = X() <caret>

// EXIST: foo
// ABSENT: bar
// ABSENT: prop
// EXIST: ext1
// ABSENT: ext2
// ABSENT: extProp
