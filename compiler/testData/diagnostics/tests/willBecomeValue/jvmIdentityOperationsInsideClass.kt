// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// WITH_STDLIB

@WillBecomeValue
class Key(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Key && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Key($x)"

    fun lock() {
        synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS_ERROR!>this<!>) { }
    }

    <!SYNCHRONIZED_ON_VALUE_CLASS_ERROR!>@Synchronized<!>
    fun lock1() {
    }

    fun identity(): Int = System.identityHashCode(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS_ERROR!>this<!>)
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression,
javaFunction, lambdaLiteral, nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast,
thisExpression */
