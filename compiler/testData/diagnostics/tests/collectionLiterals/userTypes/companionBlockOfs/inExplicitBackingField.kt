// LANGUAGE: +CollectionLiterals +CompanionBlocksAndExtensions
// RUN_PIPELINE_TILL: BACKEND

class E<T>(val t: T) {
    companion {
        operator fun <T> of(vararg ts: T): E<T> = E(ts[0])
    }
}


class WithEbf {
    val ebf: E<*>
        field = [1, 2, 3]

    fun member() {
        val asLong = ebf.t.toLong()
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, explicitBackingField, functionDeclaration, integerLiteral,
localProperty, nullableType, operator, outProjection, primaryConstructor, propertyDeclaration, starProjection,
typeParameter, vararg */
