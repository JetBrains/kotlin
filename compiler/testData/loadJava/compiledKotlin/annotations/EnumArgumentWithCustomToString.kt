//ALLOW_AST_ACCESS
package test

// This test checks that we don't accidentally call toString() on an enum value
// to determine which enum entry appears in the annotation, and call name() instead

enum class E {
    CAKE {
        override fun toString() = "LIE"
    }
}

annotation class EnumAnno(val value: E)
annotation class EnumArrayAnno(vararg val value: E)

public class EnumArgumentWithCustomToString {
    EnumAnno(E.CAKE)
    EnumArrayAnno(E.CAKE, E.CAKE)
    fun annotated() {}
}
