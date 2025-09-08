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
    a.foo = ""
    a.foo = null

    a.foo.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, interfaceDeclaration, lambdaLiteral, nullableType, override, propertyDeclaration, stringLiteral, typeConstraint,
typeParameter, typeWithExtension */
