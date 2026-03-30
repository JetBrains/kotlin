// RUN_PIPELINE_TILL: BACKEND
class A(vararg t : Int) {
    init {
        val t1 : IntArray = t
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, localProperty, primaryConstructor, propertyDeclaration, vararg */
