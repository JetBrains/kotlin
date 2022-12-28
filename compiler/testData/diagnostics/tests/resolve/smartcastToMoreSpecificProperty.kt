interface A<T : A<T>> {
    val symbol: ASymbol<T>
}

interface B<T : B<T>> : A<T> {
    override val symbol: BSymbol<T>
}

interface C : B<C> {
    fun foo()

    override val symbol: CSymbol
}

interface ASymbol<T : A<T>> {
    var value: T
}

interface BSymbol<T : B<T>> : ASymbol<T>

interface CSymbol : BSymbol<C> {
    fun bar()
}

fun test_1(symbol: BSymbol<*>) {
    if (symbol is CSymbol) {
        symbol.value.foo()
    }
}

fun test_2(b: B<*>) {
    if (b is C) {
        b.symbol.bar()
    }
}

fun <F : B<F>> test_3(b: B<F>) {
    if (b is C) {
        <!DEBUG_INFO_SMARTCAST!>b<!>.symbol.bar()
    }
}
