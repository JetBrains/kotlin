// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface UExpression {
    fun evaluate(): Any? = "fail"
}

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
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
