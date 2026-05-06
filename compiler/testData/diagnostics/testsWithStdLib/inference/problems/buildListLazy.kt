// RUN_PIPELINE_TILL: BACKEND
data class NameAndSafeValue(val name: String, val value: Int)

fun getEnv() = listOf<NameAndSafeValue>()

private val environment: List<NameAndSafeValue> by lazy {
    buildList {
        getEnv().forEach { (name, value) ->
            this += NameAndSafeValue(name, value)
        }
        sortBy { it.name }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, lambdaLiteral, localProperty, nullableType,
primaryConstructor, propertyDeclaration, propertyDelegate, thisExpression */
