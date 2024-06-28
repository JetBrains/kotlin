// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
//   Reason: K1 can't resolve calls in test_2_1 and test_2_2 functions
// SKIP_KLIB_TEST
//  Reason: AbstractKlibIrTextTestCase doen't support IGNORE_BACKEND_K1 directive
// ISSUE: KT-62863
class Bar<T>

fun Bar<Int>.specificExt() {}

fun test_1_1(x: Any) {
    x as Bar<String>
    x as Bar<Int>

    x.specificExt()
}

fun test_1_2(x: Any) {
    x as Bar<Int>
    x as Bar<String>

    x.specificExt()
}

fun <T> Bar<T>.parameterizedExt() {}

fun test_2_1(x: Any) {
    x as Bar<String>
    x as Bar<Int>

    x.parameterizedExt<String>()
    x.parameterizedExt<Int>()
}

fun test_2_2(x: Any) {
    x as Bar<Int>
    x as Bar<String>

    x.parameterizedExt<String>()
    x.parameterizedExt<Int>()
}
