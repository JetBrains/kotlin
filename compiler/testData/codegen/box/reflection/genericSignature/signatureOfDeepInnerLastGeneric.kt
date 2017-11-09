// WITH_REFLECT
// IGNORE_BACKEND: JS, NATIVE

abstract class Outer {

    inner class FirstInner {
        inner class SecondInner {
            inner class ThirdInnner {
                inner class FourthInner<A>

                fun <B> foo(): FourthInner<B> = TODO()
            }
        }
    }
}

fun box(): String {
    kotlin.test.assertEquals(
            "Outer\$FirstInner\$SecondInner\$ThirdInnner.Outer\$FirstInner\$SecondInner\$ThirdInnner\$FourthInner<B>",
            Outer.FirstInner.SecondInner.ThirdInnner::class.java.declaredMethods.single().genericReturnType.toString())

    return "OK"
}
