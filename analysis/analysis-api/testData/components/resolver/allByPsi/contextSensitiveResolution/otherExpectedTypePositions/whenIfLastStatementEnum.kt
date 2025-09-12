// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(a: MyEnum) {}

fun myRunWithMyEnum(b: () -> MyEnum) {}
fun <T> myRun(b: () -> T): T = TODO()

fun <X> id(x: X): X = TODO()

fun main(b: Boolean, i: Int) {
    val L = MyEnum.X

    val m1: MyEnum = when (i) {
        0 -> X
        1 -> {
            Y
        }
        else -> L
    }

    val m2: MyEnum =
        if (i == 0) X
        else if (i == 1) {
            Y
        }
        else L

    val m3: MyEnum = when (i) {
        0 -> when (i.hashCode()) {
            1 -> X
            else -> Y
        }
        1 -> {
            Y
        }
        else -> L
    }

    foo(
        when (i) {
            0 -> X
            1 -> {
                Y
            }
            else -> L
        }
    )

    foo(
        if (i == 0) X
        else if (i == 1) {
            Y
        }
        else L
    )

    myRunWithMyEnum {
        when (i) {
            0 -> X
            1 -> {
                Y
            }
            else -> L
        }
    }

    myRun<MyEnum> {
        when (i) {
            0 -> when (i.hashCode()) {
                1 -> X
                else -> Y
            }
            1 -> {
                Y
            }
            else -> L
        }
    }

    myRun<MyEnum> {
        when (i) {
            0 -> X
            1 -> {
                Y
            }
            else -> L
        }
    }

    myRun<MyEnum> {
        if (i == 0) X
        else if (i == 1) {
            Y
        }
        else L
    }

    myRun<MyEnum> {
        if (i == 0) X
        else if (i == 1) {
            Y
        }
        else L
    }
}
