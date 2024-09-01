// MODULE: library
// KOTLINC_ARGS: -Xjvm-default=disable
// FILE: a.kt
package base

interface UExpression {
    fun evaluate(): Any? = "fail"
}

// MODULE: main(library)
// KOTLINC_ARGS: -Xjvm-default=all-compatibility
// FILE: source.kt
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
