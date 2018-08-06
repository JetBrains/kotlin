/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 21
 DESCRIPTION: 'When' with throw expression in the control structure body.
 */

fun case_1(value: Int) {
    val lambda_1 = {
        throw Exception("Ex")
    }

    when (value) {
        1 -> lambda_1()
        2 -> throw Exception("Ex")
        3 -> {
            try {
                throw Exception("Ex")
            } catch (e: Exception) {

            }
        }
    }
}