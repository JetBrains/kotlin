// FIR_IDENTICAL

// inherits accept from FirDeclarationStatusImpl and FirResolvedDeclarationStatus
class FirResolvedDeclarationStatusImpl : FirDeclarationStatusImpl(), FirResolvedDeclarationStatus

// inherits accept from FirElement and FirDeclarationStatus
open class FirDeclarationStatusImpl : FirPureAbstractElement(), FirDeclarationStatus

abstract class FirPureAbstractElement : FirElement

interface FirResolvedDeclarationStatus : FirDeclarationStatus {
    override fun accept() {}
}

interface FirDeclarationStatus : FirElement {
    override fun accept() {}
}

interface FirElement {
    fun accept() {}
}