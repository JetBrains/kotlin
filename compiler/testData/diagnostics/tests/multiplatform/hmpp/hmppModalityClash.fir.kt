// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> abstract class Foo() {
    abstract fun <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>foo<!>()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{METADATA}!>class Impl<!> : Foo() {}

fun common() {
    Impl().foo()
}

// MODULE: intermediate()()(common)
interface I {
    fun foo()
}

expect open class Base() {}

actual abstract class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>Foo<!> : Base(), I {
    // In non-KMP world, these two f/o would squash into a single f/o final fun foo()
    // f/o abstract fun foo(): Unit in intermediate
    // f/o final fun foo(): Unit in platform
}

// MODULE: main()()(intermediate)
actual open class Base {
    fun foo() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, interfaceDeclaration, primaryConstructor */
