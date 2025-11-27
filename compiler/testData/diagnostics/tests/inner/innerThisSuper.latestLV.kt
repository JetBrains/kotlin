// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE
// LATEST_LV_DIFFERENCE

interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    class Nested {
        val t = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this@Outer<!>.bar()
        val s = super<!UNRESOLVED_LABEL!>@Outer<!>.bar()

        inner class NestedInner {
            val t = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this@Outer<!>.bar()
            val s = super<!UNRESOLVED_LABEL!>@Outer<!>.bar()
        }
    }
    
    inner class Inner {
        val t = this@Outer.bar()
        val s = super@Outer.bar()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, interfaceDeclaration, nestedClass,
propertyDeclaration, superExpression, thisExpression */
