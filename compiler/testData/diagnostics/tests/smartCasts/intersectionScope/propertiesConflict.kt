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
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = ""
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = null
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    }
}

/* GENERATED_FIR_TAGS: andExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, nullableType, override,
propertyDeclaration, smartcast, stringLiteral, typeParameter, typeWithExtension */
