class A {
    class Nested
    inner class Inner

    default object Named {
        class Nested2
        val c: Int = 1
        object Obj2

        fun foo() {
        }
    }

    object Obj
}

fun some() {
    A.Named.<caret>
}

// EXIST: Nested2
// EXIST: c
// EXIST: foo
// EXIST: Obj2
// ABSENT: Nested
// ABSENT: Named
// ABSENT: Obj