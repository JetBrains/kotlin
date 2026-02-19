// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
public interface Base {
    val test: String
        get() = "OK"
}

open class Delegate : Base {
    override val test: String
        get() = "OK"
}

fun box(): String {
    <!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object<!> : Delegate(), Base by Delegate() {

    }

    return "OK"
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, getter, inheritanceDelegation,
interfaceDeclaration, override, propertyDeclaration, stringLiteral */
