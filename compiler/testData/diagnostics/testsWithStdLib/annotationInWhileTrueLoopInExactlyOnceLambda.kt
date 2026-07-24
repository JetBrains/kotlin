// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL

class Bar<T> {
    var result: T? = null

    fun foo(): T {
        run {
            while (true) {
                when (val result = this.result) {
                    null -> {
                        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                        this as Object
                    }
                    else -> {
                        return result
                    }
                }
            }
        }
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, dnnType, equalityExpression, functionDeclaration, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, thisExpression, typeParameter,
whenExpression, whenWithSubject, whileLoop */
