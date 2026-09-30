// WITH_FIR_TEST_COMPILER_PLUGIN
// MODULE: dependency
package dependency

@org.jetbrains.kotlin.plugin.sandbox.ExternalClassWithNested
@org.jetbrains.kotlin.plugin.sandbox.DummyFunction
class Test


// MODULE: main(dependency)
package test

// main.getTopLevelGeneratedDeclarationsScope() should not contain declarations generated for the dependency module, see KT-68878
