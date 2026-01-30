// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS: FIXATION
// WITH_STDLIB
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

interface I

annotation class SerializableLike

@SerializableLike
class Impl(val x: Int): I

object Json {
    inline fun <reified T> decodeFromString(s: String): @kotlin.internal.NoInfer T = TODO()
}

inline fun <reified T: I> decodeI(s: String): T = Json.decodeFromString<T>(s)

fun <K> select(a: K, b: K) = a

fun process(s: String): I {
    return select(decodeI(s), Impl(1))
}

fun main() {
    println(process("""{"x": 1}"""))
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inline, integerLiteral, interfaceDeclaration,
multilineStringLiteral, nullableType, objectDeclaration, primaryConstructor, propertyDeclaration, reified,
typeConstraint, typeParameter */
