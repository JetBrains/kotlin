interface FirTargetElement

interface FirFunction<F : FirFunction<F>> : FirTargetElement

interface FirPropertyAccessor : FirFunction<FirPropertyAccessor>

interface FirProperty {
    val getter: FirPropertyAccessor
}

interface FirTarget<E : FirTargetElement> {
    val labeledElement: E
}

fun foo(target: FirTarget<FirFunction<*>>, property: FirProperty) {
    val functionTarget = target.labeledElement
    val x = (functionTarget <!USELESS_CAST!>as? FirFunction<!>)?.let {
        if (property.getter === functionTarget) {
            return@let 1
        }
        0
    }
}
