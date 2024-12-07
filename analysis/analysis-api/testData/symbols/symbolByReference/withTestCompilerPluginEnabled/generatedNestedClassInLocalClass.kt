// WITH_FIR_TEST_COMPILER_PLUGIN

interface A

fun local() {
    @org.jetbrains.kotlin.plugin.sandbox.AddNestedGeneratedClass
    interface Some

    Some.Gene<caret>rated
}
