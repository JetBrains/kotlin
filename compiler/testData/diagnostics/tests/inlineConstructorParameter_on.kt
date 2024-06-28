// ISSUE: KT-59664
// LANGUAGE: +ProhibitInlineModifierOnPrimaryConstructorParameters
// WITH_STDLIB

enum class Foo(open inline /* no effect */ val bar: String) {
    A("super") {
        override val bar: String
            get() = super.bar + " overridden"
    };
}

enum class Bar() {
    A {
        override val bar: String
            get() = super.bar + " overridden"
    };

    open <!DECLARATION_CANT_BE_INLINED_WARNING!>inline<!> val bar: String get() = "puper"
}

fun main() {
    println(Foo.A.bar)
}
