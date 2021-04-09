// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: privateBoundOuterClassMemberFun.kt
interface GetStep {
    fun get(): Step
}

class Outer(val k: String) {
    fun gs(x: Outer) =
        object : GetStep {
            override fun get(): Step = Step(x::test)
        }

    private fun test(s: String) = s + k
}

fun box(): String =
    Outer("!").gs(Outer("K")).get().step("O")

// FILE: Step.java
public interface Step {
    String step(String string);
}
