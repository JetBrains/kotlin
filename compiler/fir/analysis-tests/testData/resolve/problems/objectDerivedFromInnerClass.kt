// RUN_PIPELINE_TILL: FRONTEND
class Outer { open inner class Inner }
fun test() {
    val x = object : <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Outer.Inner<!>() { }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inner, localProperty,
propertyDeclaration */
