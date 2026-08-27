// WITH_FIR_TEST_COMPILER_PLUGIN
// SKIP_WHEN_OUT_OF_CONTENT_ROOT

typealias FirstAlias = SecondAlias

// An erroneous cyclic redeclaration next to the real one:
// resolution has to terminate and still reach the annotation
typealias SecondAlias = FirstAlias
typealias SecondAlias = org.jetbrains.kotlin.plugin.sandbox.MyInterfaceSupertype

@FirstAlias
cla<caret>ss A
