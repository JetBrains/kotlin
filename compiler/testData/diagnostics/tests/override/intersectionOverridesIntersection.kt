// FIR_IDENTICAL

interface FirElement {
    fun accept() {}
}

// Provides `FirElement::accept`, because has no override for it
abstract class FirPureAbstractElement : FirElement

interface FirDeclarationStatus : FirElement {
    override fun accept() {}
}

interface FirResolvedDeclarationStatus : FirDeclarationStatus {
    override fun accept() {}
}

// IDEALLY:
//     Provides `FirDeclarationStatus::accept`, because interits
//     `FirElement::accept` and `FirDeclarationStatus::accept`,
//     and the latter subsumes the former.
// REALLY:
//     Contains an IO, which is an override. So, unlike `FirPureAbstractElement`,
//     this class does have some override. We make sure this IO is green Kotlin
//     by calculating `nonSubsumed()` for the base functions
open class FirDeclarationStatusImpl : FirPureAbstractElement(), FirDeclarationStatus

// IDEALLY:
//     Provides `FirResolvedDeclarationStatus::accept`, because
//     inherits `FirDeclarationStatus::accept` and `FirResolvedDeclarationStatus::accept`,
//     and the latter subsumes the former.
// REALLY:
//     Inherits `IO FirDeclarationStatusImpl::accept` and `FirResolvedDeclarationStatus::accept`.
//     Contains an IO for the above 2 functions. To check if this is green, we unwrap the IO in
//     the base and then check `nonSubsumed()`, thus doing what is written in "IDEALLY".
class FirResolvedDeclarationStatusImpl : FirDeclarationStatusImpl(), FirResolvedDeclarationStatus
