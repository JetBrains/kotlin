// RUN_PIPELINE_TILL: FRONTEND
class A {
    val x = arrayListOf<(A<<!SYNTAX!><!>>) -> Unit>()

    // Here we got an exception during type comparison
    fun foo(){
        x.add {}
    }

}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration */
