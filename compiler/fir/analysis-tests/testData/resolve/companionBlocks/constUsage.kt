// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

annotation class Foo(val x: String)

class C {
    companion {
        const val bar = "ABC"
    }
}

@Foo(C.bar)
fun baz() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
