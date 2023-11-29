// FIR_IDENTICAL
// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.experimental.ExperimentalTypeInference

class TypeDefinition<KotlinType : Any> {
    fun parse(parser: (serializedValue: String) -> KotlinType?): Unit = TODO()
    fun serialize(parser: (value: KotlinType) -> Any?): Unit = TODO()
}

@OptIn(ExperimentalTypeInference::class)
fun <KotlinType : Any> defineType(definition: TypeDefinition<KotlinType>.() -> Unit): Unit = TODO()

fun main() {
    defineType {
        parse { it.toInt() }
        serialize { it.toString() }
    }
}
