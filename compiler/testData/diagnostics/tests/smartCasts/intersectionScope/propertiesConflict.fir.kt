// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    val foo: Any?
}

interface C: A {
    override val foo: String
}
interface B: A {
    override var foo: String?
}

fun test(a: A) {
    if (a is B && a is C) {
        a.foo = ""
        a.foo = null
        a.foo
    }
}

/* GENERATED_FIR_TAGS: andExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, nullableType, override,
propertyDeclaration, smartcast, stringLiteral, typeParameter, typeWithExtension */
