// WITH_FIR_TEST_COMPILER_PLUGIN
package foo

interface MyInterface

@org.jetbrains.kotlin.fir.plugin.MyInterfaceSupertype
class MyClass

// class: foo/MyClass