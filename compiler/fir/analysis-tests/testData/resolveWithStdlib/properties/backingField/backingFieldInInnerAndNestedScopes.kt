// RUN_PIPELINE_TILL: FRONTEND

class InnerAndNestedScope {
    val city: List<String>
        field = mutableListOf("1", "2")

    inner class Inner {
        fun ok() {
            city[0] = ""
            InnerAndNestedScope().city[0] = ""
            this@InnerAndNestedScope.city[0] = ""
        }
    }

    class Nested {
        fun foo() {
            <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>city<!>[0]
            InnerAndNestedScope().city[0] = ""
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, explicitBackingField, functionDeclaration, inner, integerLiteral,
nestedClass, nullableType, propertyDeclaration, smartcast, stringLiteral, thisExpression */
