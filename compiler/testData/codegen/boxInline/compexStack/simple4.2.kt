package test

inline fun foo(x: String) = x

fun test(a: String, s: String) = s


inline fun processRecords(block: (String, String) -> String): String {
    return test("stub", block(foo("O"), foo("K")))
}
