class C {
    fun foo(): String = ""
    fun bar(): String = ""
    fun zoo(): Any = ""
}

val c = C()
fun f() = c.<caret>foo()

// EXIST: foo
// EXIST: bar
// ABSENT: zoo