// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: privateTopLevelFun.kt
class StepProcessor {
    fun build() = Step(::test)
}

private fun test(string: String): String = string

fun box(): String =
    StepProcessor().build().step("OK")

// FILE: Step.java
public interface Step {
    String step(String string);
}
