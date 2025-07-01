// MODULE: main
// FILE: main.kt
package test

class {
    class Nested {
        val one: Int = 1
    }
}

class B {
    class Nested
}

class {
    class Nested {
        val two: Int = 2
    }
}

object C {
    object Nested
}

class {
    class Nested {
        val three: Int = 3
    }
}

class Unrelated {
    class Nested

    class {
        class Nested
    }
}

// class: test/<no name provided>.Nested
