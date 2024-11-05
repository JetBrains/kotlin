// LANGUAGE: +ContextReceivers
// WITH_STDLIB
// ISSUE: KT-55639

interface HasResolver {
    val resolver: Resolver
}

interface Resolver {
    fun resolve()
}

interface ResolverFactory {
    fun create(context: () -> Map<String, Any>): Resolver
}

fun ResolverFactory.create(context: MutableMap<String, Any>.() -> Unit) =
    create { buildMap(context) }

// ---

interface UserContext: HasResolver {
    val userId: Long
}

<!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(UserContext, MutableMap<String, Any>)
fun putUserDetails() {
    put("user_id", userId)
}
