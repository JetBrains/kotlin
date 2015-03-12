class A(val next: A? = null) {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val x: String<!>
    init {
        <!VAL_REASSIGNMENT!>next?.x<!> = "a"
    }
}

class B(val next: B? = null) {
    var x: String = next?.x ?: "default" // it's ok to use `x` of next
}
