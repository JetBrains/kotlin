import base.*

interface KotlinInterface : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}

class KotlinClass : UExpression {
    override fun evaluate(): Any? {
        return super.evaluate()
    }
}