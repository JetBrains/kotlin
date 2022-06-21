// WITH_STDLIB

interface FirExpression
interface FirNamedArgumentExpression : FirExpression {
    val expression: FirNamedArgumentExpression
}
class AnnotationUseSiteTarget

private fun List<FirExpression>.toAnnotationUseSiteTargets2(): Set<AnnotationUseSiteTarget> =
    flatMapTo(mutableSetOf()) { arg ->
        when (val unwrappedArg = if (arg is FirNamedArgumentExpression) arg.expression else arg) {
        is FirNamedArgumentExpression -> setOf()
        else -> setOf()
    }
    }