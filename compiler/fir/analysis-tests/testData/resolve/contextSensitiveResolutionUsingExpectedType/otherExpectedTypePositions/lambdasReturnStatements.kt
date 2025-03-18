// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(a: MyEnum) {}

fun myRunWithMyEnum(b: () -> MyEnum) {}
fun <T> myRun(b: () -> T): T = TODO()

fun <X> id(x: X): X = TODO()

fun main(b: Boolean) {
    val L = MyEnum.X

    myRunWithMyEnum {
        if (b) return@myRunWithMyEnum X
        Y
    }

    myRunWithMyEnum {
        if (b) return@myRunWithMyEnum id(X)
        id(Y)
    }

    myRun<MyEnum> {
        if (b) return@myRun X
        Y
    }


    foo(
        myRun {
            if (b) return@myRun X
            Y
        }
    )

    val z: MyEnum = myRun {
        if (b) return@myRun X
        Y
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>myRun<!> {
        if (b) return@myRun <!UNRESOLVED_REFERENCE!>X<!>
        <!UNRESOLVED_REFERENCE!>Y<!>
    }


    myRun {
        if (b) return@myRun MyEnum.X
        Y
    }
}
