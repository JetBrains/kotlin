// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: privateTopLevelExtFun.kt
class StepProcessor {
    fun build() = Step("O"::test)
}

private fun String.test(string: String): String =
    this + string

fun box(): String =
    StepProcessor().build().step("K")

// FILE: Step.java
public interface Step {
    String step(String string);
}
