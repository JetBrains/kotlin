// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_LAMBDA_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 8
 DESCRIPTION: 'When' with try expression in the control structure body.
 */

fun case_1(value: Int, value1: String, value2: String) {
    when (value) {
        1 -> try {
            4
        } catch (e: Exception) {
            5
        }
        2 -> try {
            throw Exception()
        } catch (e: Exception) {
            value1
        } finally {
            7
        }
        3 -> try {
            try {
                throw Exception()
            } catch (e: Exception) {
                {value2}
            }
        } catch (e: Exception) {
            {2}
        }
        4 -> {
            try {
                4
            } catch (e: Exception) {
                5
            }
        }
    }
}