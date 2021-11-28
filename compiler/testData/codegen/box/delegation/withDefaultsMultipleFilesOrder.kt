// FILE: lib.kt
interface ResolutionScope {
    fun getContributedDescriptors(s: String = "OK"): String
}

// processing deprecatedScopes.kt before scopes.kt should show that there is no problem in processing delegated members after fake overrides

// FILE: deprecatedScopes.kt
abstract class DeprecatedLexicalScope(a: LexicalScope) : LexicalScope by a

// FILE: scopes.kt
interface LexicalScope : ResolutionScope

// FILE: main.kt
class ScopeImpl : LexicalScope {
    override fun getContributedDescriptors(s: String): String = s
}
class Impl : DeprecatedLexicalScope(ScopeImpl())

fun box(): String = Impl().getContributedDescriptors()
