// RUN_PIPELINE_TILL: FRONTEND
class A {
    val x: Int = 1
        get() {
            ::<!UNSUPPORTED!>field<!>
            return field
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, propertyDeclaration */
