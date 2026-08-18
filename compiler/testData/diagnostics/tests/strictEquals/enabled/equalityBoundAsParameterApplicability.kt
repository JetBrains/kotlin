// RUN_PIPELINE_TILL: FRONTEND

fun topLevelFun(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?) {}

class ConstructorApplicaibitliy(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?)

class ConstructorApplicaibitliyVal(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> val other: Any?)

class SecondaryConstructorApplicaibitliyVal {
    constructor(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?) {}
}

var propertySetter: Any? = null
    set(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> value) { field = value }


fun lambdaParam(f: (<!WRONG_ANNOTATION_TARGET!>@EqualityBound(Any::class)<!> Any?) -> Unit) {}

object Scope {
    fun lambdaParamAtCallsite(f: (Any?) -> Unit) {}

    fun test() {
        lambdaParamAtCallsite { <!WRONG_ANNOTATION_TARGET!>@EqualityBound(Any::class)<!> <!UNRESOLVED_REFERENCE!>other<!> <!SYNTAX!>-><!> }
        lambdaParamAtCallsite(fun(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> param: Any?) {})
    }
}

context(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?)
fun contextFun() {}

fun localChecks() {
    fun localFun(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?) {}

    context(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?)
    fun localContextFun() {}

    class ConstructorsCheck(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?) {
        constructor(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?, other2: Any?) : this(other)
    }
}

class LooksLikeEquals {
    fun equals(<!UNSUPPORTED!>@EqualityBound(Any::class)<!> other: Any?, f: String): Boolean = true
}
