// WITH_FIR_TEST_COMPILER_PLUGIN
package test

@org.jetbrains.kotlin.plugin.sandbox.DummyFunction
class Test

fun test() {
    <expr>dummyTest</expr>(Test())
}
