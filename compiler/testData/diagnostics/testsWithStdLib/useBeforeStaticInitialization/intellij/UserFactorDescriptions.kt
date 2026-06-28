// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface UserFactorDescription<U, R> {
    val factorId: String
    val updaterFactory: (Any) -> U
    val readerFactory: (Any) -> R
}

object UserFactorDescriptions {
    private val IDS: MutableSet<String> = mutableSetOf()

    val COMPLETION_TYPE: UserFactorDescription<Any, Any> =
        Descriptor.register("completionType", { it }, { it })
    val COMPLETION_FINISH_TYPE: UserFactorDescription<Any, Any> =
        Descriptor.register("completionFinishedType", { it }, { it })
    val COMPLETION_USAGE: UserFactorDescription<Any, Any> =
        Descriptor.register("completionUsage", { it }, { it })
    val PREFIX_LENGTH_ON_COMPLETION: UserFactorDescription<Any, Any> =
        Descriptor.register("prefixLength", { it }, { it })
    val SELECTED_ITEM_POSITION: UserFactorDescription<Any, Any> =
        Descriptor.register("itemPosition", { it }, { it })
    val TIME_BETWEEN_TYPING: UserFactorDescription<Any, Any> =
        Descriptor.register("timeBetweenTyping", { it }, { it })
    val MNEMONICS_USAGE: UserFactorDescription<Any, Any> =
        Descriptor.register("mnemonicsUsage", { it }, { it })
    val PREFIX_MATCHING_TYPE: UserFactorDescription<Any, Any> =
        Descriptor.register("prefixMatchingType", { it }, { it })
    val TEMPLATES_USAGE: UserFactorDescription<Any, Any> =
        Descriptor.register("templatesUsage", { it }, { it })

    fun isKnownFactor(id: String): Boolean = id in IDS

    private class Descriptor<U, R> private constructor(
        override val factorId: String,
        override val updaterFactory: (Any) -> U,
        override val readerFactory: (Any) -> R) : UserFactorDescription<U, R> {
        companion object {
            fun <U, R> register(factorId: String,
                               updaterFactory: (Any) -> U,
                               readerFactory: (Any) -> R): UserFactorDescription<U, R> {
                require(!isKnownFactor(factorId)) { "Descriptor with id '$factorId' already exists" }
                IDS.add(factorId)
                return Descriptor(factorId, updaterFactory, readerFactory)
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nestedClass, nullableType, objectDeclaration, override, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter */
