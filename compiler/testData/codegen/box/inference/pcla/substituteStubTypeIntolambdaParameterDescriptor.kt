// WITH_STDLIB
// ISSUE: KT-64823 (K2/PCLA difference)
// Also, see testData/diagnostics/testsWithStdLib/coroutines/inference/kt36220.kt
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: error: the type of a receiver hasn't been inferred yet. Please specify type argument for generic parameter 'T' of 'defineType' explicitly

import kotlin.experimental.ExperimentalTypeInference

class TypeDefinition<K : Any> {
    fun parse(parser: (serializedValue: String) -> K?): Unit {}
    fun serialize(parser: (value: K) -> Any?): Unit {}
}

@OptIn(ExperimentalTypeInference::class)
fun <T : Any> defineType(definition: TypeDefinition<T>.() -> Unit): Unit {}

fun test() {
    defineType {
        parse { it as Int }
        serialize { it.toString() }
    }
}

fun box(): String {
    test()
    return "OK"
}
