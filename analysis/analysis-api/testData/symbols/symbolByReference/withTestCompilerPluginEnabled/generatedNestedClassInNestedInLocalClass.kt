// WITH_FIR_TEST_COMPILER_PLUGIN

fun local() {
    interface Container {
        @org.jetbrains.kotlin.fir.plugin.AddNestedGeneratedClass
        interface Some
    }

    Container.Some.Gene<caret>rated
}
