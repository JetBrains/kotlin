// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53113
abstract class AbstractCollectionAssert<SELF : AbstractCollectionAssert<SELF, ELEMENT>, ELEMENT> {
    fun describedAs(s: String): SELF = TODO()
    fun anyMatch(x: (ELEMENT) -> Boolean) {}
}

fun <E> assertThat(actual: Collection<E>): AbstractCollectionAssert<*, E> = TODO()

fun main(strings: Collection<String>) {
    assertThat(strings)
        .describedAs("2-length strings")
        .anyMatch { it.length == 2 }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, equalityExpression, functionDeclaration, functionalType,
integerLiteral, lambdaLiteral, nullableType, starProjection, stringLiteral, typeConstraint, typeParameter */
