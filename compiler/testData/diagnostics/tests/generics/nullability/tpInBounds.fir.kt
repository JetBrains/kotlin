// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VALUE -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -VARIABLE_WITH_REDUNDANT_INITIALIZER

class A<T : CharSequence?, E1 : T, E2: T?> {
    fun T.bar() {}

    fun foo(x: E1, y: E2) {
        x.bar()

        if (1 == 1) {
            y<!UNSAFE_CALL!>.<!>bar()
        }

        x?.bar()
        y?.bar()


        var t: T = x
        var tN: T? = y

        // condition needed to make smart cast on tN impossible
        if (1 == 1) {
            tN = x
        }

        if (1 == 1) {
            t = <!ASSIGNMENT_TYPE_MISMATCH!>tN<!>
        }

        t = <!ASSIGNMENT_TYPE_MISMATCH!>y<!>

        if (y != null) {
            t = y
        }

        if (tN != null) {
            t = tN
        }
    }
}
