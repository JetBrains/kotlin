// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-64823 (K2/PCLA difference)

class TypeDefinition<KotlinType : Any> {
    fun parse(parser: (serializedValue: String) -> KotlinType?): Unit = TODO()
    fun serialize(parser: (value: KotlinType) -> Any?): Unit = TODO()
}

fun <KotlinType : Any> defineType(definition: TypeDefinition<KotlinType>.() -> Unit): Unit = TODO()

fun main() {
    defineType {
        parse { it.toInt() }
        serialize {
            val i = it
            i.toString()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, typeConstraint, typeParameter, typeWithExtension */
