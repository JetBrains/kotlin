// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions

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
