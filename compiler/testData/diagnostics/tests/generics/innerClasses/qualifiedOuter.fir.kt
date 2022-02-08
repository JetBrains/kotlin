// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

class Outer<T> {
    inner class Inner
    fun foo(x: Outer<String>.Inner, y: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.Inner, z: Inner) {
        var inner = Inner()
        x.checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Inner>() }
        x.checkType { _<Outer<String>.Inner>() }
        z.checkType { _<Inner>() }
        z.checkType { _<Outer<T>.Inner>() }

        inner = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>
    }

    class Nested
    fun bar(x: Outer.Nested) {
        var nested = Nested()
        nested = x

        x.checkType { _<Nested>() }
    }
}
