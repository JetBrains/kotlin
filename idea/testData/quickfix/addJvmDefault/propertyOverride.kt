// "Add '@JvmDefault' annotation" "true"
// JVM_TARGET: 1.8
// COMPILER_ARGUMENTS: -Xjvm-default=enable
// WITH_RUNTIME
interface Foo {
    @JvmDefault
    val foo: String
        get() = ""
}
interface Bar: Foo {

    <caret>override val foo: String
        get() = ""
}
