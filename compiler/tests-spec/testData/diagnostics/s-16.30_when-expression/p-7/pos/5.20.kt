/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 20
 DESCRIPTION: 'When' with bound value and this expression in 'when condition'.
 */

open class A {
    val __prop_1 = 1
    val __lamdba_1 = {1}

    fun __fun_1(): Int {
        return 1
    }

    // CASE DESCRIPTION: 'When' with 'else' branch (as expression).
    fun case_1(value: Any?): String = when (value) {
        this -> ""
        ((this)) -> ""
        this::__prop_1.get() -> ""
        this.__prop_1 -> ""
        this.__lamdba_1() -> ""
        this::__lamdba_1.get()() -> ""
        this.__fun_1() -> ""
        this::__fun_1.invoke() -> ""
        else -> ""
    }

    // CASE DESCRIPTION: 'When' without 'else' branch (as statement).
    fun case_2(value: Any?): String {
        when (value) {
            this -> return ""
            ((this)) -> return ""
            this::__prop_1.get() -> return ""
            this.__prop_1 -> return ""
            this.__lamdba_1() -> return ""
            this::__lamdba_1.get()() -> return ""
            this.__fun_1() -> return ""
            this::__fun_1.invoke() -> return ""
        }

        return ""
    }
}
