// Uses 1 StringBuilder
fun test1(s1: String, s2: String, s3: String) =
        (s1 + s2) + s3

// Uses 1 StringBuilder
fun test2(s1: String, s2: String, s3: String) =
        s1 + (s2 + s3)

// Uses 1 StringBuilder
fun test3(s1: String, s2: String, s3: String, s4: String) =
        ((s1 + s2) + ((s3 + s4)))

// Combination of String.plus and string literal
// Uses 1 StringBuilder
fun test4(s1: String, s2: String, s3: String) =
        "s1: $s1; " +
        "s2: $s2; " +
        "s3: $s3"

// Combination of String.plus and nested string literal
// Uses 1 StringBuilder
fun test5(s1: String, s2: String, s3: String) =
        "${"s1:" + "${" " + s1};"}; " +
        "${"s2:" + "${" " + s2};"}; " +
        "${"s3:" + "${" " + s3}"}"

// Top-level string concatenation element is a string literal
// Uses 1 StringBuilder
fun test6(s1: String, s2: String, s3: String) =
    "${"s1:" + "${" " + s1};"} ${"s2:" + "${" " + s2};"} ${"s3:" + "${" " + s3}"}"

// Uses 3 StringBuilders:
// - In return expression
// - In argument to 1st call to foo()
// - In argument to 2nd call to foo() inside string literal
fun test7(s1: String, s2: String, s3: String): String {
    fun foo(s: String) = s
    return "foo: " + foo(s1 + s2 + " ${foo("\${s3.length} = ${s3.length}")}")
}

// 9 NEW java/lang/StringBuilder