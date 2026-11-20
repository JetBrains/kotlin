// IGNORE_STANDALONE
// KT-91917
// `LLStandaloneFirElementByPsiElementChooser` doesn't support `dynamic` types
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtFunction

public inline fun Any?.asDynamic(): dynamic = this
