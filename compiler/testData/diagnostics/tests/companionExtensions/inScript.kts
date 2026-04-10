// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: test.kts

class C {
}

companion fun C.foo() {}
companion val C.bar = 1
companion var C.baz = 2

C.foo()
C.bar
C.baz = 3

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, init, integerLiteral, propertyDeclaration */
