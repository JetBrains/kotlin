// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-29853

// KT-29853: Declaring local variable named `field` inside property getter triggers name shadowing warning

class A {
    val propertyWithoutBackingField: Any
        get() {
            val field = 1
            return field * 2
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, localProperty, multiplicativeExpression,
propertyDeclaration */
