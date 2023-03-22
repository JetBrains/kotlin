// IGNORE_REVERSED_RESOLVE
class A {
    init {
        <!RETURN_NOT_ALLOWED!>return<!>
        <!RETURN_NOT_ALLOWED!>return<!> 1
    }
    constructor() {
        if (1 == 1) {
            return
            return <!RETURN_TYPE_MISMATCH!>1<!>
        }
        return
        return <!RETURN_TYPE_MISMATCH!>foo()<!>
    }

    fun foo(): Int = 1
}
