// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib0
// RETURN_VALUE_CHECKER_MODE: CHECKER
// FILE: Super.kt

@MustUseReturnValues
interface Super<T> {
    fun x(): T
    @IgnorableReturnValue fun y(): T
}

interface NonAnnotated<T> {
    fun z(): T
}

// MODULE: lib1(lib0)
// RETURN_VALUE_CHECKER_MODE: FULL
// FILE: Lib.kt

@MustUseReturnValues
interface U: Super<Unit>, NonAnnotated<Unit> {
    override fun x()
    override fun <!OVERRIDING_IGNORABLE_WITH_MUST_USE!>y<!>()
    override fun z()
}

@MustUseReturnValues
interface S: Super<String>, NonAnnotated<String> {
    override fun x(): String
    override fun <!OVERRIDING_IGNORABLE_WITH_MUST_USE!>y<!>(): String
    override fun z(): String
}

fun main(s: S, u: U) {
    u.x()
    u.y()
    u.z()
    s.<!RETURN_VALUE_NOT_USED!>x<!>()
    s.<!RETURN_VALUE_NOT_USED!>y<!>()
    s.<!RETURN_VALUE_NOT_USED!>z<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, override, typeParameter */
