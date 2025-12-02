// ISSUE: KT-82844
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.0,2.1,2.2,2.3.0
// ^^^ KT-82844 is fixed in 2.3.20-Beta1

fun box(): String {
    var a = 1
    when (a++) {}
    return if (a == 2) "OK" else "Fail: $a"
}
