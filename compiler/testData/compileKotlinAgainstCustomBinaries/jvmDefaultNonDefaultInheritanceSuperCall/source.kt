import base.*

interface KotlinEvaluatableUElement : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}