// WITH_FIR_TEST_COMPILER_PLUGIN
interface Target {
    fun value(): String
}

@org.jetbrains.kotlin.plugin.sandbox.AddSupertype(Target::class)
interface MergePoint {
    fun v<caret>alue(): String
}