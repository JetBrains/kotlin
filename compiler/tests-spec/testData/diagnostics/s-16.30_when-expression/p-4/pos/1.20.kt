// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 20
 DESCRIPTION: 'When' with this expression in the control structure body.
 */

class A {
    val __prop_1 = 1
    var __prop_2 = 2
    val __lambda_1 = {1}

    fun __fun_1(): Int {
        return 1
    }

    fun case_1(value: Int) {
        when (value) {
            1 -> this
            2 -> ((this))
            3 -> this::__prop_1.get()
            4 -> this.__prop_1
            5 -> this.__lambda_1()
            6 -> this::__lambda_1.get()()
            7 -> this.__fun_1()
            8 -> this::__fun_1.invoke()
        }
    }
}