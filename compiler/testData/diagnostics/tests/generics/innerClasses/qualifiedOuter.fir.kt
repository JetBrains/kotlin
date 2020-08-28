// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

class Outer<T> {
    inner class Inner
    fun foo(x: Outer<String>.Inner, y: Outer.Inner, z: Inner) {
        var inner = Inner()
        x.checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Inner>() }
        x.checkType { _<Outer<String>.Inner>() }
        z.checkType { _<Inner>() }
        z.checkType { _<Outer<T>.Inner>() }

        inner = x
    }

    class Nested
    fun bar(x: Outer.Nested) {
        var nested = Nested()
        nested = x

        x.checkType { _<Nested>() }
    }
}
