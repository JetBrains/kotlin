class A {
    init {
        <!RETURN_NOT_ALLOWED!>return<!>
        <!RETURN_NOT_ALLOWED!>return<!> 1
    }
    constructor() {
        if (1 == 1) {
            return
            return 1
        }
        return
        return <!TYPE_MISMATCH!>foo()<!>
    }

    fun foo(): Int = 1
}
