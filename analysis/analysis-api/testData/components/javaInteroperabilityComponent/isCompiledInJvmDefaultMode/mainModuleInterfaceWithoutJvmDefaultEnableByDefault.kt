// class: Foo
// LANGUAGE: -JvmDefaultEnableByDefault

// Without an explicit `-jvm-default` mode, the language version defines it: `disable` before Kotlin 2.2
interface Foo {
    fun foo() {}
}
