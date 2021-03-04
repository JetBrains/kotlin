class ASTNode
class Wrap(val message: String)

typealias WrappingStrategy = (childElement: ASTNode) -> Wrap?

fun getWrappingStrategy(): WrappingStrategy {
    return wrap@{ childElement ->
        Wrap("OK").let {
            return@wrap it
        }
    }
}

fun getWrapAfterAnnotation(childElement: ASTNode): Wrap? {
    return Wrap("OK")
}

fun box(): String {
    return getWrappingStrategy().invoke(ASTNode())?.message ?: "fail"
}
