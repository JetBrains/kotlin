// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_VARIABLE

class TestClass {
    companion object {
        inline operator fun <T> invoke(task: () -> T) = task()
    }
}

fun test(s: String): String {
    val a = TestClass { "K" }
    a checkType { _<String>() }

    <!UNREACHABLE_CODE!>val b =<!> TestClass { return s }
    <!UNREACHABLE_CODE!>b checkType { _<Nothing>() }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, inline, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration,
stringLiteral, typeParameter, typeWithExtension */
