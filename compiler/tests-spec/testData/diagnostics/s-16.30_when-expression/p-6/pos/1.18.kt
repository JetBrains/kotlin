// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_LAMBDA_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 18
 DESCRIPTION: 'When' with lambda literal in the control structure body.
 */

fun case_1(value: Int) {
    val __lambda_1 = { 0 }

    when (value) {
        1 -> __lambda_1
        2 -> {{{{}}}}
        3 -> { -> (Int)
            {
                arg: Int -> {
                    { println(arg) }
                }
            }
        }
    }
}