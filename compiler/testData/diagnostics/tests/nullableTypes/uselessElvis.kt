// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -SENSELESS_COMPARISON, -DEBUG_INFO_SMARTCAST

fun <T: Any?> test1(t: Any?): Any {
    return t <!UNCHECKED_CAST!>as T<!> ?: ""
}

fun <T: Any> test2(t: Any?): Any {
    return t <!UNCHECKED_CAST!>as T<!> <!USELESS_ELVIS!>?: ""<!>
}

fun <T: Any?> test3(t: Any?): Any {
    if (t != null) {
        return t <!USELESS_ELVIS!>?: ""<!>
    }

    return 1
}

fun takeNotNull(s: String) {}
fun <T> notNull(): T = TODO()
fun <T> nullable(): T? = null
fun <T> dependOn(x: T) = x

fun test() {
    takeNotNull(notNull() ?: "")
    takeNotNull(nullable() ?: "")

    val x: String? = null
    takeNotNull(dependOn(x) ?: "")
    takeNotNull(dependOn(dependOn(x)) ?: "")
    takeNotNull(dependOn(dependOn(x as String)) <!USELESS_ELVIS!>?: ""<!>)

    if (x != null) {
        takeNotNull(dependOn(x) <!USELESS_ELVIS!>?: ""<!>)
        takeNotNull(dependOn(dependOn(x)) <!USELESS_ELVIS!>?: ""<!>)
        takeNotNull(dependOn(dependOn(x) <!USELESS_CAST!>as? String<!>) ?: "")
    }

    takeNotNull(bar()!!)
}

inline fun <reified T : Any> reifiedNull(): T? = null

fun testFrom13648() {
    takeNotNull(reifiedNull() ?: "")
}

fun bar() = <!UNRESOLVED_REFERENCE!>unresolved<!>

fun testUselessElvisLeftIsNull(l: List<String>) {
    val result1 = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> "default"
    val result2 = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> 42
    val result3 = <!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> l

    takeNotNull(<!USELESS_ELVIS_LEFT_IS_NULL!>null ?:<!> "value")

    // Should not trigger - left is not null literal
    val x: String? = null
    val result4 = x ?: "default"

    // Should not trigger - left is not null literal
    val result5 = nullable() ?: "default"
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, elvisExpression, equalityExpression, functionDeclaration,
ifExpression, inline, integerLiteral, localProperty, nullableType, propertyDeclaration, reified, smartcast,
stringLiteral, typeConstraint, typeParameter, uselessElvisLeftIsNull */
