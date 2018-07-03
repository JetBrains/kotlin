// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 20
 DESCRIPTION: 'When' with this expression in the control structure body.
 */

class A {
    val prop_1 = 1
    val lambda_1 = {1}
    fun fun_1(): Int { return 1 }

    fun case_1(value: Int) {
        when {
            value == 1 -> this
            value == 2 -> ((this))
            value == 3 -> this::prop_1.get()
            value == 4 -> this.prop_1
            value == 5 -> this.lambda_1()
            value == 6 -> this::lambda_1.get()()
            value == 7 -> this.fun_1()
            value == 8 -> this::fun_1.invoke()
        }
    }
}