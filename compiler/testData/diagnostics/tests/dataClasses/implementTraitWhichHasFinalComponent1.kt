interface T {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> fun component1(): Int = 42
}

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class A(val x: Int) : T
