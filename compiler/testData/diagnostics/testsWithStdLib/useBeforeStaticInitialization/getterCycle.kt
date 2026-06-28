// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object A {
    <!POSSIBLY_UNINITIALIZED_PROPERTY!>val x = <!ACCESSING_POSSIBLY_UNINITIALIZED_PROPERTY!>y<!><!>
    val y: Any get() = x
}

/* GENERATED_FIR_TAGS: getter, objectDeclaration, propertyDeclaration */
