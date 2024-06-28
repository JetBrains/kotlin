// ISSUE: KT-66350

annotation class TestParameters(val param: String)

object TestVeConsts {
    const val LETS_GO_BUTTON = 1
}

val ConstsDuplicate = TestVeConsts

@TestParameters(
    <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"{ veId: ${ConstsDuplicate.LETS_GO_BUTTON}}"<!>
)
fun box() = "OK"

// Additional examples

annotation class WithDefaultValue(val value: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>ConstsDuplicate.LETS_GO_BUTTON + 1<!>)
