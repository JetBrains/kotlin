import base.*

interface KotlinEvaluatableUElement : UExpression {
    override fun evaluate(): Any? {
        return "OK"
    }
}

abstract class KotlinAbstractUExpression() : UExpression {}

@JvmDefaultWithoutCompatibility
class KotlinUBinaryExpressionWithType : KotlinAbstractUExpression(), KotlinEvaluatableUElement {}

fun box(): String {
    val foo = KotlinUBinaryExpressionWithType()
    return foo.evaluate() as String
}
