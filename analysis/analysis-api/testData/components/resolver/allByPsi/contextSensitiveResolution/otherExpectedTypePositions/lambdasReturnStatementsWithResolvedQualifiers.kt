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

    myRun {
        if (b) return@myRun X
        Y
    }
    myRun {
        if (b) return@myRun MySealed.X
        Y
    }
}
