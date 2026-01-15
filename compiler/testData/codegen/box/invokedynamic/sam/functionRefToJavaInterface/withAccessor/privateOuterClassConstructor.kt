// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// FILE: privateOuterClassConstructor.kt
interface GetStep {
    fun get(): Step
}

class Outer {
    private val ok: String

    private constructor(ok: String) {
        this.ok = ok
    }

    constructor() {
        this.ok = "xxx"
    }

    val obj = object : GetStep {
        override fun get(): Step = Step(::Outer)
    }

    override fun toString() = ok
}

fun box() =
    Outer().obj.get().step("OK").toString()


// FILE: Step.java
public interface Step {
    Object step(String string);
}
