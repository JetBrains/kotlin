// ISSUE: KT-82844

fun box(): String {
    var a = 1
    when (a++) {}
    return if (a == 2) "OK" else "Fail: $a"
}
