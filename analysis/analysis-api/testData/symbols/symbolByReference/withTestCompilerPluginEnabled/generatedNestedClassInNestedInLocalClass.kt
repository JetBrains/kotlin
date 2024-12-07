// WITH_FIR_TEST_COMPILER_PLUGIN

fun local() {
    interface Container {
        @org.jetbrains.kotlin.plugin.sandbox.AddNestedGeneratedClass
        interface Some
    }

    Container.Some.Gene<caret>rated
}
