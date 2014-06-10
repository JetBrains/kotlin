import kotlin.platform.*

var v: Int = 1
    [platformName("vget")]
    get
    [platformName("vset")]
    set

fun box(): String {
    v += 1
    if (v != 2) return "Fail: $v"

    return "OK"
}