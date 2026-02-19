// MODULE: main
// FILE: main.kt
package test

class A {
    class {
        val one: Int = 1
    }
}

class B {
    class { }
}

class A {
    class {
        val two: Int = 2
    }
}

object C {
    object { }
}

class A {
    class {
        val three: Int = 3
    }
}

class Unrelated {
    class { }

    class A {
        class { }
    }
}

// class: test/A.<no name provided>
