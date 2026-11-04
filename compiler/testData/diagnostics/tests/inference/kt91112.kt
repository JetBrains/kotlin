// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-91112

inline fun <reified T : Enum<T>, V> ((T) -> V).find(value: V): T? {
    return enumValues<T>().firstOrNull { this(it) == value }
}

enum class Animal(val string: String) {
    Cat("cat"),
    Dog("dog");

    companion object {
        fun fromString(string: String) = Animal::string.find(string)
            ?: throw IllegalArgumentException("Bad animal: '$string'")
    }
}

fun <T : Enum<T>> g(f: (T) -> String): T? = null
fun <T : Enum<T>> gNotNull(f: (T) -> String): T = null!!
fun <K> sel(x: K, y: K): K = x

fun f1() = g(Animal::string) ?: throw Exception()
fun f2() = sel(gNotNull(Animal::string), throw Exception())

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, companionObject, elvisExpression, enumDeclaration, enumEntry,
equalityExpression, funWithExtensionReceiver, functionDeclaration, functionalType, inline, lambdaLiteral, nullableType,
objectDeclaration, primaryConstructor, propertyDeclaration, reified, stringLiteral, thisExpression, typeConstraint,
typeParameter */
