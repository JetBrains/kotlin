// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-43707
// WITH_STDLIB
// FIR_DUMP

// KT-43707: Nothing type is inferred for contravariant type when split or merged with null
class Contravariance<in T>

fun foo() {
    val result = if (true) {
        findElement()
    } else {
        null
    }

    val result2 = findElement()
    val result3 = findElement() ?: return
}

fun findElement(): Contravariance<*>? {
    TODO()
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, functionDeclaration, ifExpression, in, localProperty,
nullableType, propertyDeclaration, starProjection, typeParameter */
