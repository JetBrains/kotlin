// WITH_FIR_TEST_COMPILER_PLUGIN
// MODULE: dependency
package dependency

@org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested
@org.jetbrains.kotlin.plugin.sandbox.DummyFunction
class Test


// MODULE: main(dependency)
package test

// This test is currently broken, see KT-68878
// The reasoning: main.getTopLevelGeneratedDeclarationsScope() should not contain declarations generated for dependency module
