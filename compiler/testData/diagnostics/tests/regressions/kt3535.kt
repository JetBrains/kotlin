// RUN_PIPELINE_TILL: BACKEND
// KT-3535 Functional value-parametr in nested class is inaccessible

class Foo {
    class Bar(val p: (Any) -> Any) {
        fun f() {
            p(1)
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, nestedClass,
primaryConstructor, propertyDeclaration */
