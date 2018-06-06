// "Add '@JvmDefault' annotation" "true"
// JVM_TARGET: 1.8
// COMPILER_ARGUMENTS: -Xjvm-default=enable
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
