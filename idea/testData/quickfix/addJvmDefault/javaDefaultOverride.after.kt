// "Add '@JvmDefault' annotation" "true"
// COMPILER_ARGUMENTS: -Xenable-jvm-default
// WITH_RUNTIME
interface Bar : Foo {
    <caret>@JvmDefault
    override fun foo() {

    }
}
