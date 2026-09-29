// IGNORE_FIR
// WITH_FIR_TEST_COMPILER_PLUGIN
fun foo() {
    @org.jetbrains.kotlin.plugin.sandbox.AllOpen
    class Top {
        enum class NestedEnum {
            Entry;
        }

        class NestedClass
    }
}
