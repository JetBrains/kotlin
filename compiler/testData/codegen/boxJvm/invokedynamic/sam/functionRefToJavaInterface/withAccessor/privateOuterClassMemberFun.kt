// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: privateOuterClassMemberFun.kt
interface GetStep {
    fun get(): Step
}

class Outer(val k: String) {
    val obj = object : GetStep {
        override fun get(): Step = Step(::test)
    }

    private fun test(s: String) = s + k
}

fun box(): String =
    Outer("K").obj.get().step("O")

// FILE: Step.java
public interface Step {
    String step(String string);
}
