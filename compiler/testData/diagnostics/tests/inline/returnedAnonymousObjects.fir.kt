// !LANGUAGE: -ApproximateAnonymousReturnTypesInPrivateInlineFunctions

private inline fun foo1(crossinline f: () -> Int) = object {
    fun bar(): Int = f()
}

interface I1
interface I2

private inline fun foo2(crossinline f: () -> Int) = object : I1 {
    fun bar(): Int = f()
}

private inline fun foo3(crossinline f: () -> Int) = object : I1, I2 {
    fun bar(): Int = f()
}

private fun foo4(f: () -> Int) = object {
    fun bar(): Int = f()
}

fun test1(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>foo1 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>foo1 { 2 }<!>
    }
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
}

fun test2(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("I1")!>foo2 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("I1")!>foo2 { 2 }<!>
    }
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
}

fun test3(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot hide local type object : R|I1|, R|I2| {    private constructor(): R|<anonymous>| {        super<R|kotlin/Any|>()    }    public final fun bar(): R|kotlin/Int| {        ^bar R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Int|>|()    }}")!>foo3 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Cannot hide local type object : R|I1|, R|I2| {    private constructor(): R|<anonymous>| {        super<R|kotlin/Any|>()    }    public final fun bar(): R|kotlin/Int| {        ^bar R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Int|>|()    }}")!>foo3 { 2 }<!>
    }
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
}

fun test4(b: Boolean) {
    var x = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>")!>foo4 { 1 }<!>
    if (b) {
        x = <!DEBUG_INFO_EXPRESSION_TYPE("<anonymous>")!>foo4 { 2 }<!>
    }
    x.bar()
}
