// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM

var initOrder = ""

enum class Foo(val f: String) {
    Alpha(run {
        initOrder += "1"
        "alpha"
    });

    companion {
        val blockProp: String = run {
            initOrder += "2"
            "block"
        }
    }
}

fun box(): String {
    // Trigger initialization by accessing an enum entry
    val a = Foo.Alpha

    if (a.f != "alpha") return "FAIL: Alpha.f=${a.f}"

    val b = Foo.blockProp

    if (b != "block") return "FAIL: blockProp=$b"

    // §3.2.2: enum entries are initialized first, then
    // implicit members (entries/values), then companion blocks
    // follow program order.
    if (initOrder != "12") return "FAIL: initOrder=$initOrder"

    return "OK"
}
