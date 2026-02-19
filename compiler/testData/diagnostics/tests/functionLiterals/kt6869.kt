// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun main() {
    var list = listOf(1)

    val a: Int? = 2

    a?.let { list += it }
}

operator fun <T> Iterable<T>.plus(element: T): List<T> = null!!
fun <T> listOf(vararg values: T): List<T> = null!!

/* GENERATED_FIR_TAGS: additiveExpression, assignment, checkNotNullCall, funWithExtensionReceiver, functionDeclaration,
integerLiteral, lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration, safeCall, typeParameter,
vararg */
