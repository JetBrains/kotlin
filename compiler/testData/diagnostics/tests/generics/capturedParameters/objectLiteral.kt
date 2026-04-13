// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun <T> magic(): T = null!!

class Q {
    private fun <E> foo() =
        object {
            val prop: E = magic()
        }

    private var x = foo<CharSequence>()
    private var y = foo<String>()

    fun bar() {
        x <!ASSIGNMENT_TYPE_MISMATCH!>=<!> y
        x = foo<CharSequence>()
        y = foo<String>()

        x.prop.checkType { _<CharSequence>() }
        y.prop.checkType { _<String>() }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration,
funWithExtensionReceiver, functionDeclaration, functionalType, infix, lambdaLiteral, nullableType, propertyDeclaration,
typeParameter, typeWithExtension */
