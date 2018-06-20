// "Add '@JvmDefault' annotation" "true"
// COMPILER_ARGUMENTS: -Xjvm-default=enable
// WITH_RUNTIME
interface Bar : Foo {
    <caret>override fun foo() {

    }
}
