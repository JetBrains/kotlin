// !WITH_NEW_INFERENCE
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
        return foo()
    }

    fun foo(): Int = 1
}
