// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

class Outer<T> {
    inner class Inner
    fun foo(x: Outer<String>.Inner, y: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Outer<!>.Inner, z: Inner) {
        var inner = Inner()
        x.checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><Inner>() }
        x.checkType { _<Outer<String>.Inner>() }
        z.checkType { _<Inner>() }
        z.checkType { _<Outer<T>.Inner>() }

        inner = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>x<!>
    }

    class Nested
    fun bar(x: Outer.Nested) {
        var nested = Nested()
        nested = x

        x.checkType { _<Nested>() }
    }
}
