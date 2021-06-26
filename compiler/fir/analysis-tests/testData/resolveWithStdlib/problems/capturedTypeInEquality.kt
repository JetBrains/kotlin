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
    val x = (functionTarget as? FirFunction)?.let {
        if (<!EQUALITY_NOT_APPLICABLE_WARNING!>property.getter === functionTarget<!>) {
            return@let 1
        }
        0
    }
}
