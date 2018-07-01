// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_LAMBDA_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 8
 DESCRIPTION: 'When' with try expression in the control structure body.
 */

fun case_1(value: Int, value1: String, value2: String) {
    when {
        value == 1 -> try {
            4
        } catch (e: Exception) {
            5
        }
        value == 2 -> try {
            throw Exception()
        } catch (e: Exception) {
            value1
        } finally {
            7
        }
        value == 3 -> try {
            try {
                throw Exception()
            } catch (e: Exception) {
                {value2}
            }
        } catch (e: Exception) {
            {2}
        }
        value == 4 -> {
            try {
                4
            } catch (e: Exception) {
                5
            }
        }
    }
}