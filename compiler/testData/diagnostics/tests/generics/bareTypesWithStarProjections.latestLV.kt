// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// SKIP_TXT
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER

interface FirMemberDeclaration : FirDeclaration
interface TypeConstructorMarker

interface FirElement
interface FirDeclaration : FirElement
interface FirStatement : FirElement
interface FirSymbolOwner<E> : FirElement where E : FirSymbolOwner<E>, E : FirDeclaration

interface FirCallableDeclaration<F : FirCallableDeclaration<F>> : FirDeclaration, FirSymbolOwner<F>

interface FirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration
interface AbstractFirBasedSymbol<E> : FirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration

interface FirVariable<F : FirVariable<F>> : FirCallableDeclaration<F>, FirDeclaration, FirStatement

interface FirCallableSymbol<D : FirCallableDeclaration<D>> : AbstractFirBasedSymbol<D>
interface FirVariableSymbol<D : FirVariable<D>> : FirCallableSymbol<D>
interface FirPropertySymbol : FirVariableSymbol<FirProperty>

interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> : FirCallableDeclaration<F>, FirMemberDeclaration

interface FirProperty : FirVariable<FirProperty>, FirCallableMemberDeclaration<FirProperty>

fun <D> AbstractFirBasedSymbol<D>.phasedFir(): D where D : FirDeclaration, D : FirSymbolOwner<D> = TODO()

fun foo(coneSymbol: AbstractFirBasedSymbol<*>) {
    if (coneSymbol !is <!NO_TYPE_ARGUMENTS_ON_RHS!>FirVariableSymbol<!>) return
    // For bare types, smart cast should be performed to FirVariableSymbol<*> not to FirVariableSymbol<out FirVariable<*>>
    coneSymbol.phasedFir() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><FirVariable<*>>() }

    if (coneSymbol !is FirPropertySymbol) return
    coneSymbol.phasedFir() checkType { _<FirProperty>() }
}
