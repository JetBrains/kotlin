// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59664
// LANGUAGE: +ProhibitInlineModifierOnPrimaryConstructorParameters
// WITH_STDLIB

enum class Foo(<!INLINE_PROPERTY_WITH_BACKING_FIELD_DEPRECATION_ERROR!>open <!WRONG_MODIFIER_TARGET!>inline<!> /* no effect */ val bar: String<!>) {
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

    open <!DECLARATION_CANT_BE_INLINED!>inline<!> val bar: String get() = "puper"
}

fun main() {
    println(Foo.A.bar)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, getter, primaryConstructor, propertyDeclaration,
stringLiteral */
