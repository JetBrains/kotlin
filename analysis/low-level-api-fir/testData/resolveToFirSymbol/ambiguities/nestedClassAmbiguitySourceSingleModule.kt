// MODULE: main
// FILE: main.kt
package test

class A {
    class Nested {
        val one: Int = 1
    }
}

class B {
    class Nested
}

class A {
    class Nested {
        val two: Int = 2
    }
}

object C {
    object Nested
}

class A {
    class Nested {
        val three: Int = 3
    }
}

class Unrelated {
    class Nested

    class A {
        class Nested
    }
}

// class: test/A.Nested
