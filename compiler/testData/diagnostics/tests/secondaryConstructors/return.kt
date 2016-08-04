class A {
    init {
        <!RETURN_NOT_ALLOWED!>return<!>
        <!UNREACHABLE_CODE!><!RETURN_NOT_ALLOWED!>return<!> 1<!>
    }
    constructor() <!UNREACHABLE_CODE!>{
        if (1 == 1) {
            return
            return <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
        }
        return
        return <!TYPE_MISMATCH!>foo()<!>
    }<!>

    fun foo(): Int = 1
}
