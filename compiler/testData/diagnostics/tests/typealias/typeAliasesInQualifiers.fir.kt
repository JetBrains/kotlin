// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: foo.kt

package test

typealias ClassAlias = ClassSample
typealias ObjectAlias = ObjectSample
typealias EnumAlias = EnumSample

class ClassSample {
    class Nested {
        fun func() {}
    }

    fun func() {}
}

object ObjectSample {
    class Nested {
        fun func() {}
    }

    fun func() {}
}

enum class EnumSample {
    Entry;

    class Nested {
        fun func() {}
    }

    fun func() {}
}

// FILE: test.kt

fun foo(
    a0: test.ClassSample.Nested,
    a1: <!UNRESOLVED_REFERENCE!>test.ClassAlias.Nested<!>,

    b0: test.ObjectSample.Nested,
    b1: <!UNRESOLVED_REFERENCE!>test.ObjectAlias.Nested<!>,

    c0: test.EnumSample.Nested,
    c1: <!UNRESOLVED_REFERENCE!>test.EnumAlias.Nested<!>
) {
    test.ClassSample::Nested
    test.ClassAlias::Nested

    test.ClassSample::func
    test.ClassAlias::func

    test.ClassSample.Nested::func
    <!UNRESOLVED_REFERENCE!>test.ClassAlias.<!UNRESOLVED_REFERENCE!>Nested<!>::func<!>

    test.ObjectSample::Nested
    test.ObjectAlias::Nested

    test.ObjectSample::func
    test.ObjectAlias::func

    test.ObjectSample.Nested::func
    <!UNRESOLVED_REFERENCE!>test.ObjectAlias.<!UNRESOLVED_REFERENCE!>Nested<!>::func<!>

    test.EnumSample::Nested
    test.EnumAlias::Nested

    test.EnumSample::func
    test.EnumAlias::func

    test.EnumSample.Nested::func
    <!UNRESOLVED_REFERENCE!>test.EnumAlias.<!UNRESOLVED_REFERENCE!>Nested<!>::func<!>
}
