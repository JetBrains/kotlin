// RUN_PIPELINE_TILL: CODEGEN
class A(vararg val t : Int) {
    init {
        val t1 : IntArray = t
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, localProperty, primaryConstructor, propertyDeclaration, vararg */
