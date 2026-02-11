// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
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
    <!UNRESOLVED_REFERENCE!>put<!>("user_id", <!UNRESOLVED_REFERENCE!>userId<!>)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, functionalType,
interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, stringLiteral, typeWithExtension */
