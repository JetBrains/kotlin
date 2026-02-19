// RUN_PIPELINE_TILL: BACKEND
class Foo(name: () -> String) {
    val result = run { name() }

    val name = result.length
}

fun bar(name: () -> String) {
    val result = run { name() }

    val name = result.length
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
primaryConstructor, propertyDeclaration */
