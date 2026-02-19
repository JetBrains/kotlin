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

    val m1: MyEnum = try {
        X
    } catch(e: Exception) {
        Y
    } catch (t: Throwable) {
        L
    }

    val m2: MyEnum = try {
        X
    } catch(e: Exception) {
        try {
            X
        } catch(e: Exception) {
            Y
        } catch (t: Throwable) {
            L
        }
    } catch (t: Throwable) {
        L
    }

    foo(
        try {
            X
        } catch(e: Exception) {
            Y
        } catch (t: Throwable) {
            L
        }
    )

    myRunWithMyEnum {
        try {
            X
        } catch(e: Exception) {
            Y
        } catch (t: Throwable) {
            L
        }
    }

    myRun<MyEnum> {
        try {
            X
        } catch(e: Exception) {
            Y
        } catch (t: Throwable) {
            L
        }
    }
}
