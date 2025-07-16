// RUN_PIPELINE_TILL: BACKEND
class B {
    fun append() {}
}

class A {
    val message = B()

    fun foo(w: Boolean) {
        if (w) {
            val message = ""
            message
        } else {
            message.append() // message here should relate to the class-level property
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, localProperty, propertyDeclaration,
stringLiteral */
