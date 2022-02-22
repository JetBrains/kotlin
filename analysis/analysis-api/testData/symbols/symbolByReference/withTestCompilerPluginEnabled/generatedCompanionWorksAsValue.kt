// WITH_FIR_TEST_COMPILER_PLUGIN
package test

@org.jetbrains.kotlin.fir.plugin.CompanionWithFoo
class WithGeneratedCompanion

fun test() {
    val companionObject = WithGenerated<caret>Companion
}