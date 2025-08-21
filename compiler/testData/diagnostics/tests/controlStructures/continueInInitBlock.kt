// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47892

fun test(b: Boolean)  {
    while (b) {
        class A {
            init {
                continue
            }
            constructor(): super()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, continue, functionDeclaration, init, localClass, secondaryConstructor,
whileLoop */
