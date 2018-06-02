// "Add '@JvmDefault' annotation" "true"
// JVM_TARGET: 1.8
// COMPILER_ARGUMENTS: -Xenable-jvm-default
// WITH_RUNTIME
interface Foo {
    @JvmDefault
    fun foo() {

    }
}
interface Bar: Foo {

    <caret>override fun foo() {

    }
}
