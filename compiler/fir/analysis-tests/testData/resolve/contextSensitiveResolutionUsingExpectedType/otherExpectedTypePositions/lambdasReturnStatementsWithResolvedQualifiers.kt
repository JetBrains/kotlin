// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class MySealed {
    data object X : MySealed()
    data object Y : MySealed()
}

fun foo(a: MySealed) {}

fun myRunWithMySealed(b: () -> MySealed) {}
fun <T> myRun(b: () -> T): T = TODO()

fun <X> id(x: X): X = TODO()

fun main(b: Boolean) {
    val L = MySealed.X

    myRunWithMySealed {
        if (b) return@myRunWithMySealed X
        Y
    }

    myRunWithMySealed {
        if (b) return@myRunWithMySealed id(X)
        id(Y)
    }

    myRun<MySealed> {
        if (b) return@myRun X
        Y
    }


    foo(
        myRun {
            if (b) return@myRun X
            Y
        }
    )

    val z: MySealed = myRun {
        if (b) return@myRun X
        Y
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>myRun<!> {
        if (b) return@myRun <!UNRESOLVED_REFERENCE!>X<!>
        <!UNRESOLVED_REFERENCE!>Y<!>
    }


    myRun {
        if (b) return@myRun MySealed.X
        <!ARGUMENT_TYPE_MISMATCH!>Y<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed, typeParameter */
