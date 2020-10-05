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

@KotlinMagicConstant(intValues = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>[StepRequest.STEP_INTO.<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>toLong()<!>, StepRequest.STEP_OVER.<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>toLong()<!>, StepRequest.STEP_OUT.<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>toLong()<!>]<!>)
val kotlinDepth: Int = 42
