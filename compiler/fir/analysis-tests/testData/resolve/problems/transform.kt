sealed class CompositeTransformResult<out T : Any> {

    class Single<out T : Any>(val _single: T) : CompositeTransformResult<T>()

    class Multiple<out T : Any>(val _list: List<T>) : CompositeTransformResult<T>()

    companion object {
        fun <T : Any> single(t: T) = Single(t)
        fun <T : Any> many(l: List<T>) = Multiple(l)
    }

    val single: T
        get() = (this as Single<*>)._single as T
}

interface FirElement {
    fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R

    fun accept(visitor: FirVisitorVoid) = accept(visitor, null)

    fun <E : FirElement, D> transform(visitor: FirTransformer<D>, data: D): CompositeTransformResult<E>
}

abstract class FirVisitor<out R, in D>

abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>()

abstract class FirTransformer<in D> : FirVisitor<CompositeTransformResult<FirElement>, D>()

interface FirAnnotationContainer : FirElement {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirStatement : FirAnnotationContainer {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirTypeParameterRefsOwner : FirElement {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirDeclaration : FirElement {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirAnnotatedDeclaration : FirDeclaration, FirAnnotationContainer {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirSymbolOwner<E> : FirElement where E : FirSymbolOwner<E>, E : FirDeclaration {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirClassLikeDeclaration<F : FirClassLikeDeclaration<F>> : FirAnnotatedDeclaration, FirStatement, FirSymbolOwner<F>

interface FirClass<F : FirClass<F>> : FirClassLikeDeclaration<F>, FirStatement, FirTypeParameterRefsOwner

private class FirApplySupertypesTransformer() : FirTransformer<Nothing?>()

fun <F : FirClass<F>> F.runSupertypeResolvePhaseForLocalClass(): F {
    val applySupertypesTransformer = FirApplySupertypesTransformer()
    return this.transform<F, Nothing?>(applySupertypesTransformer, null).single
}

abstract class FirPureAbstractElement : FirElement

interface FirTypedDeclaration : FirAnnotatedDeclaration {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

interface FirCallableDeclaration<F : FirCallableDeclaration<F>> : FirTypedDeclaration, FirSymbolOwner<F> {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

abstract class FirVariable<F : FirVariable<F>> : FirPureAbstractElement(), FirCallableDeclaration<F>, FirAnnotatedDeclaration, FirStatement {
    abstract override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R
}

abstract class FirWhenExpression {
    abstract val subjectVariable: FirVariable<*>?
}

class FirRenderer : FirVisitorVoid() {
    fun foo(expression: FirWhenExpression) {
        val variable = expression.subjectVariable
        if (variable != null) {
            variable.accept(this)
        }
    }
}
