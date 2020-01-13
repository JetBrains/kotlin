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
    a1: test.ClassAlias.Nested,

    b0: test.ObjectSample.Nested,
    b1: test.ObjectAlias.Nested,

    c0: test.EnumSample.Nested,
    c1: test.EnumAlias.Nested
) {
    test.ClassSample::Nested
    test.ClassAlias::Nested

    test.ClassSample::func
    test.ClassAlias::func

    test.ClassSample.Nested::func
    test.ClassAlias.Nested::func

    test.ObjectSample::Nested
    test.ObjectAlias::Nested

    test.ObjectSample::func
    test.ObjectAlias::func

    test.ObjectSample.Nested::func
    test.ObjectAlias.Nested::func

    test.EnumSample::Nested
    test.EnumAlias::Nested

    test.EnumSample::func
    test.EnumAlias::func

    test.EnumSample.Nested::func
    test.EnumAlias.Nested::func
}