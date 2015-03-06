class A {
    class Nested
    inner class Inner

    class object {
        class Nested2
        val c: Int = 1
        object Obj2

        fun foo() {
        }
    }

    object Obj
}

fun some() {
    A.<caret>
}

// EXIST: Nested
// EXIST: Default
// EXIST: c
// EXIST: foo
// EXIST: Obj
// ABSENT: Nested2
// ABSENT: Obj2