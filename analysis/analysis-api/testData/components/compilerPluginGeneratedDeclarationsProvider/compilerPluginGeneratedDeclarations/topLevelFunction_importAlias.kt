// WITH_FIR_TEST_COMPILER_PLUGIN
package test

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction as Dummy

@Dummy
class Test

fun regularTopLevelFunction() {}
