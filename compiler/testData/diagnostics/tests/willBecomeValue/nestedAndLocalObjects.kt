// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

class Regular {
    @WillBecomeValue
    object NestedObject {
        override fun toString(): String = "NestedObject"
    }

    @WillBecomeValue
    class NestedClass(val x: Int) {
        override fun equals(other: Any?): Boolean = other is NestedClass && other.x == x
        override fun hashCode(): Int = x
        override fun toString(): String = "NestedClass($x)"
    }
}

fun local() {
    @WillBecomeValue
    <!LOCAL_OBJECT_NOT_ALLOWED!>object <!WILL_BECOME_VALUE_CLASS_NOT_TOP_LEVEL!>LocalObject<!><!> {
        override fun toString(): String = "LocalObject"
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression,
localClass, nestedClass, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration,
smartcast, stringLiteral */
