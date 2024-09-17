// FIR_IDENTICAL

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
interface Interface

fun foo() {
    // error: inheriting Interface requires an explicit opt-in
    open class LocalOpenKlassA: <!OPT_IN_USAGE_ERROR!>Interface<!>
    abstract class LocalAbstractKlassA: <!OPT_IN_USAGE_ERROR!>Interface<!>
    class LocalKlassA: <!OPT_IN_USAGE_ERROR!>Interface<!>
    data class LocalDataKlassA(val arg: Int): <!OPT_IN_USAGE_ERROR!>Interface<!>
    object: <!OPT_IN_USAGE_ERROR!>Interface<!> {}

    // opt-in is present, no errors
    @OptIn(ApiMarker::class) open class LocalOpenKlassB: Interface
    @OptIn(ApiMarker::class) abstract class LocalAbstractKlassB: Interface
    @OptIn(ApiMarker::class) class LocalKlassB: Interface
    @OptIn(ApiMarker::class) data class LocalDataKlassB(val arg: Int): Interface
    @OptIn(ApiMarker::class) object: Interface {}

    // requiring to opt-in into local classifiers works as well
    // (even though it doesn't make that much sense)
    @ApiMarker open class LocalOpenKlassC: Interface
    @ApiMarker abstract class LocalAbstractKlassC: Interface
    @ApiMarker class LocalKlassC: Interface
    @ApiMarker data class LocalDataKlassC(val arg: Int): Interface
    @ApiMarker object: Interface {}
}
