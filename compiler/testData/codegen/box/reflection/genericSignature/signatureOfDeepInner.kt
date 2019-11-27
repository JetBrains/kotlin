// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_REFLECT

abstract class Outer {

    inner class FirstInner {
        inner class SecondInner<A> {
            inner class ThirdInnner {
                inner class FourthInner

                fun foo(): FourthInner = TODO()
            }
        }
    }
}

fun box(): String {
    kotlin.test.assertEquals(
        "Outer\$FirstInner\$SecondInner<A>\$ThirdInnner\$FourthInner",
        Outer.FirstInner.SecondInner.ThirdInnner::class.java.declaredMethods.single().genericReturnType.toString()
    )
    return "OK"
}
