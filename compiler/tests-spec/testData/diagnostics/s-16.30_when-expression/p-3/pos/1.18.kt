// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_LAMBDA_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 18
 DESCRIPTION: 'When' with lambda literal in the control structure body.
 */

fun case_1(value: Int) {
    val lambda1 = { 0 }

    when {
        value == 1 -> lambda1
        value == 2 -> {{{{}}}}
        value == 3 -> { -> (Int)
            {
                arg: Int -> {
                    { println(arg) }
                }
            }
        }
    }
}