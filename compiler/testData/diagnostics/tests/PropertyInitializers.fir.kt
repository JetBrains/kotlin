// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class Foo(val a: Int, b: Int) {
    val c = a + b

    val d: Int
        get() = a

    val e: Int
        get() = <!UNRESOLVED_REFERENCE!>b<!>

    val map: Map<String, Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> mapOf(1 to "hello")
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, getter, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
