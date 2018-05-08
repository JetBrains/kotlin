// "Add '@JvmDefault' annotation" "true"
// JVM_TARGET: 1.8
// COMPILER_ARGUMENTS: -Xenable-jvm-default
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
