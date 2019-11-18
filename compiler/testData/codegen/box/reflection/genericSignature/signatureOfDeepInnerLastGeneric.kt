// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_REFLECT

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
        "Outer\$FirstInner\$SecondInner\$ThirdInnner\$FourthInner<B>",
        Outer.FirstInner.SecondInner.ThirdInnner::class.java.declaredMethods.single().genericReturnType.toString()
    )

    return "OK"
}
