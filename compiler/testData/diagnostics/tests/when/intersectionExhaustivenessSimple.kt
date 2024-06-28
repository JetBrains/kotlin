// FIR_IDENTICAL
// SKIP_TXT
// CHECK_TYPE

sealed class KtClassifierSymbol

interface KtNamedSymbol

abstract class KtTypeParameterSymbol : KtClassifierSymbol() {}

sealed class KtClassLikeSymbol : KtClassifierSymbol() {}

fun foo(symbol: KtClassifierSymbol) {
    if (symbol !is KtNamedSymbol) return
    val x = when (symbol) {
        is KtClassLikeSymbol -> "1"
        is KtTypeParameterSymbol -> "2"
    }

    x checkType { _<String>() }
}
