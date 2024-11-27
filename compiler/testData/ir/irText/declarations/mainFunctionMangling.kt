// FIR_IDENTICAL
// WITH_STDLIB

// FILE: a.kt
fun main() {
    println("main()")
}

// Looks like `main` but is not a main function.
fun <T> main(t: T): T {
    println("main(T): T")
    return t
}

class C {
    fun main() {
        println("C.main()")
    }

    // Looks like `main` but is not a main function.
    fun <T> main(t: T): T {
        println("C.main(T): T")
        return t
    }
}

object O {
    fun main() {
        println("O.main()")
    }

    // Looks like `main` but is not a main function.
    fun <T> main(t: T): T {
        println("O.main(T): T")
        return t
    }
}

// FILE: b.kt
package foo

fun main() {
    println("foo.main()")
}

// Looks like `main` but is not a main function.
fun <T> main(t: T): T {
    println("foo.main(T): T")
    return t
}

class C {
    fun main() {
        println("foo.C.main()")
    }

    // Looks like `main` but is not a main function.
    fun <T> main(t: T): T {
        println("foo.C.main(T): T")
        return t
    }
}

object O {
    fun main() {
        println("foo.O.main()")
    }

    // Looks like `main` but is not a main function.
    fun <T> main(t: T): T {
        println("foo.O.main(T): T")
        return t
    }
}
