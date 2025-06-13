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

fun <T> test(a: T) where T : B, T : C {
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = ""
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = null

    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, interfaceDeclaration, lambdaLiteral, nullableType, override, propertyDeclaration, stringLiteral, typeConstraint,
typeParameter, typeWithExtension */
