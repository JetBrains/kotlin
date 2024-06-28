// FIR_DUMP
// Similar to kt36220.kt, but with receivers instead of it
// ISSUE: KT-64823 (K2/PCLA difference)

class TypeDefinition<KotlinType : Any> {
    fun parse(parser: (serializedValue: String) -> KotlinType?): Unit = TODO()
    fun serialize(parser: KotlinType.() -> Any?): Unit = TODO()
}

fun <KotlinType : Any> defineType(definition: TypeDefinition<KotlinType>.() -> Unit): Unit = TODO()

fun foo() {
    defineType {
        parse { it.toInt() }
        serialize <!CANNOT_INFER_PARAMETER_TYPE!>{ toString() }<!>
    }
}

fun bar() {
    defineType {
        parse { it.toInt() }
        serialize <!CANNOT_INFER_PARAMETER_TYPE!>{ <!CANNOT_INFER_PARAMETER_TYPE!>this<!>.toString() }<!>
    }
}
