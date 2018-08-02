
/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 7: Any other expression.
 NUMBER: 20
 DESCRIPTION: 'When' with bound value and this expression in 'when condition'.
 */

open class A {
    val prop1 = 1
    val lamdba1 = {1}

    fun fun_1(): Int {
        return 1
    }

    // CASE DESCRIPTION: 'When' with 'else' branch (as expression).
    fun case_1(value: Any?): String = when (value) {
        this -> ""
        ((this)) -> ""
        this::prop1.get() -> ""
        this.prop1 -> ""
        this.lamdba1() -> ""
        this::lamdba1.get()() -> ""
        this.fun_1() -> ""
        this::fun_1.invoke() -> ""
        else -> ""
    }

    // CASE DESCRIPTION: 'When' without 'else' branch (as statement).
    fun case_2(value: Any?): String {
        when (value) {
            this -> return ""
            ((this)) -> return ""
            this::prop1.get() -> return ""
            this.prop1 -> return ""
            this.lamdba1() -> return ""
            this::lamdba1.get()() -> return ""
            this.fun_1() -> return ""
            this::fun_1.invoke() -> return ""
        }

        return ""
    }
}
