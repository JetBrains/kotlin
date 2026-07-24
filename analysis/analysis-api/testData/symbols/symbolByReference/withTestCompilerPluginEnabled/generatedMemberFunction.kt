// WITH_FIR_TEST_COMPILER_PLUGIN

package test

@org.jetbrains.kotlin.plugin.sandbox.NestedClassAndMaterializeMember class MyClass

fun test(m: MyClass) {
    m.materi<caret>alize()
}
