// WITH_FIR_TEST_COMPILER_PLUGIN

interface A

fun local() {
    @org.jetbrains.kotlin.fir.plugin.AddNestedGeneratedClass
    interface Some

    Some.Gene<caret>rated
}
