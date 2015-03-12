class A {
    init {
        <!RETURN_NOT_ALLOWED!>return<!>
        <!RETURN_NOT_ALLOWED, UNREACHABLE_CODE!>return 1<!>
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
