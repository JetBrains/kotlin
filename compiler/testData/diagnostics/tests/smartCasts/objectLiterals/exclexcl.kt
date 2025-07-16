// RUN_PIPELINE_TILL: BACKEND
abstract class Runnable {
    abstract fun run()
}

fun foo(): Int {
    val c: Int? = null
    val a: Int? = 1
    if (c is Int) {
        val k = object: Runnable() {
            init {
                a!!
            }
            override fun run() = Unit
        }
        k.run()
        val d: Int = <!DEBUG_INFO_SMARTCAST!>c<!>
        // a is not null because of k constructor, but we do not know it
        return a <!UNSAFE_OPERATOR_CALL!>+<!> d
    }
    else return -1
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, checkNotNullCall, classDeclaration,
functionDeclaration, ifExpression, init, integerLiteral, isExpression, localProperty, nullableType, override,
propertyDeclaration, smartcast */
