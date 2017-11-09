// WITH_REFLECT
// IGNORE_BACKEND: JS, NATIVE

abstract class Outer {

    inner class FirstInner {
        inner class SecondInner<A> {
            inner class ThirdInnner {
                inner class FourthInner<B>

                fun <C> foo(): FourthInner<C> = TODO()
            }
        }
    }
}

fun box(): String {
    kotlin.test.assertEquals(
            "Outer\$FirstInner.Outer\$FirstInner\$SecondInner<A>.ThirdInnner.FourthInner<C>",
            Outer.FirstInner.SecondInner.ThirdInnner::class.java.declaredMethods.single().genericReturnType.toString())

    return "OK"
}
