// COMPILATION_ERRORS
// KT-90376
package test.fqNames

data class X(val a: Int, val b: Int)

val (topA, topB) = X(1, 2)

class C {
    val (memberA, memberB) = X(1, 2)

    class Nested {
        val (nestedA, nestedB) = X(1, 2)
    }

    companion object {
        val (companionA, companionB) = X(1, 2)
    }
}

object O {
    val (objectA, objectB) = X(1, 2)
}

enum class E {
    ENTRY {
        val (enumEntryA, enumEntryB) = X(1, 2)
    };
}

class {
    val (namelessA, namelessB) = X(1, 2)
    val namelessProperty = 1
}

fun foo() {
    val (localA, localB) = X(1, 2)

    class Local {
        val (localClassA, localClassB) = X(1, 2)
    }
}
