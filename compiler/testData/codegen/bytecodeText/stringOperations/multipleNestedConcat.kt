// Uses 3 StringBuilders:
// - In return expression
// - In argument to 1st call to foo()
// - In argument to 2nd call to foo() inside string literal
fun test(s1: String, s2: String, s3: String): String {
    fun foo(s: String) = s
    return "foo: " + foo(s1 + s2 + " ${foo("\${s3.length} = ${s3.length}")}")
}

// 3 NEW java/lang/StringBuilder