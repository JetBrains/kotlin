// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNCHECKED_CAST
// Issues: KT-38890, KT-38439

fun foo(x: () -> Int) {}

fun <T>id(x: T) = x

// Before the fix, there wasn't any type mismatch error in NI due to result type not being a subtype of expected type
fun main() {
    val x: () -> Int = { "" }

    val a0: () -> Int = fun(): String = "1"
    val a1: () -> Int = (fun() = "1")
    val a2: () -> Unit = (fun() = "1")
    val a3: Unit = (fun() = "1")
    val a4 = (fun() = "1")
    val a5 = (fun(): String = "1")
    val a6: () -> Int = (fun() = 1)
    val a7: () -> Int = (fun(): String = "1") as () -> Int
    val a8: () -> Int = fun(): String = "1"
    val a9: () -> () -> () -> Int = fun(): () -> () -> String = fun(): () -> String = fun(): String = "1"

    <!INAPPLICABLE_CANDIDATE!>foo<!>(fun(): String = "1")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(((fun(): String = "1")))

    val a10: Int.(String) -> Int = fun (x: String) = 10
    val a11: () -> () -> () -> Int = fun() = fun() = fun(): String = "1"

    val a12: Int = fun(): String = "1"
    val a13: Int = fun() = fun(): String = "1"
    val a14: Int = fun() = fun() = "1"
    val a15: Int = fun() = fun() = {}
    val a16: Int = fun() = fun() {}

    val a17: () -> Unit = fun() {}
    val a18: () -> Int = fun() {}
    val a19: () -> () -> Int = fun() = fun() {}
    val a20: () -> () -> () -> Unit = fun() = fun() = {}
    val a21: () -> () -> () -> Int = fun() = fun() = {}
}
