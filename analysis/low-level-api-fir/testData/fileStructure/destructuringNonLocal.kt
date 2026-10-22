/* RootStructureElement */data class X(val a: Int, val b: Int)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

val (a, b) = X(1, 2)/* DeclarationStructureElement */

class A {/* ClassDeclarationStructureElement */
    val (c, d) = X(1, 2)/* DeclarationStructureElement */

    class Nested {/* ClassDeclarationStructureElement */
        val [e, f] = X(1, 2)/* DeclarationStructureElement */
    }
}

enum class E {/* ClassDeclarationStructureElement */
    ENTRY {/* DeclarationStructureElement */
        val (g, h) = X(1, 2)
    };
}

fun foo() {/* DeclarationStructureElement */
    class Local {
        val (i, j) = X(1, 2)
    }

    val o = object {
        val (k, l) = X(1, 2)
    }
}
