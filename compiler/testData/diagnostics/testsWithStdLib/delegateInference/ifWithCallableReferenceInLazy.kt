// ISSUE: KT-58754

fun foo() {}
fun bar() {}

class Test(b: Boolean) {
    private val test_1 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>lazy<!> {
        val a = if (b) {
            <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!>
        } else {
            <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!><!>
        }
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    }<!>

    private val test_2 by lazy {
        val a = if (b) ::foo else ::bar
        a
    }

    private val test_3 by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>lazy<!> {
        val a = when {
            b -> { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!> }
            else -> { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!><!> }
        }
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    }<!>

    private val test_4 by lazy {
        val a = when {
            b -> ::foo
            else -> ::bar
        }
        a
    }
}

