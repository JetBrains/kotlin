// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

open class View

fun test() {
    val target = foo<View>() ?: foo() ?: run {}
}

fun <T : View> foo(): T? {
    return null
}

/* GENERATED_FIR_TAGS: classDeclaration, elvisExpression, functionDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, typeConstraint, typeParameter */
