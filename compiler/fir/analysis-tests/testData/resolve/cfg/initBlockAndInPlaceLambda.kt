// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface B

interface A {
    val b: B?
}

class C(a: A, b: B) {
    init {
        val c = a.b?.let {
            C(a, it)
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, interfaceDeclaration, lambdaLiteral, localProperty, nullableType,
primaryConstructor, propertyDeclaration, safeCall */
