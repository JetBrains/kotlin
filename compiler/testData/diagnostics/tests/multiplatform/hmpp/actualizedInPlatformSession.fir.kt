// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class Foo() {
    fun foo()
}

// MODULE: intermediate()()(common)
expect open class Base() {}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS{METADATA}!>Foo<!> : Base() {
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, primaryConstructor */
