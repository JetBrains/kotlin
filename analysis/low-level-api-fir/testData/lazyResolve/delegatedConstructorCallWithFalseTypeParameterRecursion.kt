// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-77082
// LANGUAGE:-DontMakeExplicitNullableJavaTypeArgumentsFlexible

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
abstract class Foo<T : Foo.T> {
    interface T
}

// MODULE: main(lib)
// FILE: Bar.kt
class Ba<caret>r : Foo<BarT>()

class BarT : Foo.T
