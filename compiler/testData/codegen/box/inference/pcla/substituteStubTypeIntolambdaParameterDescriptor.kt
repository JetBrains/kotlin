// WITH_STDLIB
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-64823 (K2/PCLA difference)
// Also, see testData/diagnostics/testsWithStdLib/coroutines/inference/kt36220.kt

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
