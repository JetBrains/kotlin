// RUN_PIPELINE_TILL: FRONTEND
// JDK_KIND: MODIFIED_MOCK_JDK
// CHECK_TYPE

interface A : MutableCollection<String> {
    // Override of deprecated function could be marked as deprecated too
    override fun <!OVERRIDE_DEPRECATION!>nonExistingMethod<!>(x: String) = ""
}

fun foo(x: MutableCollection<Int>, y: Collection<String>, z: A) {
    x.<!DEPRECATION!>nonExistingMethod<!>(1).checkType { _<String>() }
    y.<!DEPRECATION!>nonExistingMethod<!>("")
    z.<!DEPRECATION!>nonExistingMethod<!>("")
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, interfaceDeclaration, lambdaLiteral, nullableType, override, stringLiteral, typeParameter,
typeWithExtension */
