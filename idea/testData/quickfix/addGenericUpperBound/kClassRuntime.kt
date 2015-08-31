// "Add 'kotlin.Any' as upper bound for E" "true"

inline fun <reified /* abc */   E> bar() = E::class.java<caret>
