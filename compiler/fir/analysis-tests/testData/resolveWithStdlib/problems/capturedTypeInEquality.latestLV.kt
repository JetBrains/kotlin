// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
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
    val x = (functionTarget as? <!NO_TYPE_ARGUMENTS_ON_RHS!>FirFunction<!>)?.<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>let<!> <!CANNOT_INFER_PARAMETER_TYPE!>{
        if (property.getter === functionTarget) {
            return@let 1
        }
        0
    }<!>
}
