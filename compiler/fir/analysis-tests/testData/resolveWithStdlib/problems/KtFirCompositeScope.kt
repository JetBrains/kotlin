interface KtScope {
    fun getAllNames(): Set<String>
}

inline fun <E> buildSet(@<!EXPERIMENTAL_API_USAGE_ERROR!>BuilderInference<!> builderAction: MutableSet<E>.() -> Unit): Set<E> {
    return null!!
}

inline fun <R> withValidityAssertion(action: () -> R): R {
    return action()
}

class KtFirCompositeScope(val subScopes: List<KtScope>) {
    fun getAllNames(): Set<String> = withValidityAssertion {
        buildSet {
            subScopes.flatMapTo(this) { it.getAllNames() }
        }
    }
}