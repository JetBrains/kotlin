// IGNORE_REVERSED_RESOLVE
// FILE: MagicConstant.java

public @interface MagicConstant {
    long[] intValues() default {};
}

// FILE: kt42346.kt

class StepRequest {
    companion object {
        const val STEP_INTO = 0
        const val STEP_OVER = 1
        const val STEP_OUT = 2
    }
}

@MagicConstant(intValues = [StepRequest.STEP_INTO.toLong(), StepRequest.STEP_OVER.toLong(), StepRequest.STEP_OUT.toLong()])
val depth: Int = 42

annotation class KotlinMagicConstant(val intValues: LongArray)

@KotlinMagicConstant(intValues = [StepRequest.STEP_INTO.toLong(), StepRequest.STEP_OVER.toLong(), StepRequest.STEP_OUT.toLong()])
val kotlinDepth: Int = 42
