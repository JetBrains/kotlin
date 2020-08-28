fun foo() {
    <!CAN_BE_VAL!>var<!> a = 1
    var b = <!VARIABLE_INITIALIZER_IS_REDUNDANT!>2<!>
    var c = 3

    for (i in 0..5) {
        if (a == 2) {
            <!ASSIGNED_VALUE_IS_NEVER_READ!>b<!> = c
            c = a
        } else {
            b = a
            c = b
        }
    }

    if (c == a) {
        foo()
    }
}