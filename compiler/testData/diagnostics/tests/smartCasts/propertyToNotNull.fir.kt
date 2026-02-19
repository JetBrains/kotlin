// RUN_PIPELINE_TILL: FRONTEND
class Immutable(val x: String?) {
    fun foo(): String {
        if (x != null) return x
        return ""
    }
}

class Mutable(var y: String?) {
    fun foo(): String {
        if (y != null) return <!SMARTCAST_IMPOSSIBLE!>y<!>
        return ""
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, nullableType,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
