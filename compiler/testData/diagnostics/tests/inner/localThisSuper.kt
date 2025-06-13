// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Trait {
    fun bar() = 42
}

class Outer : Trait {
    fun foo() {
        val t = this@Outer
        val s = super@Outer.bar()

        class Local : Trait {
            val t = this@Outer
            val s = super@Outer.bar()

            inner class Inner {
                val t = this@Local
                val s = super@Local.bar()

                val tt = this@Outer
                val ss = super@Outer.bar()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, interfaceDeclaration, localClass,
localProperty, propertyDeclaration, superExpression, thisExpression */
