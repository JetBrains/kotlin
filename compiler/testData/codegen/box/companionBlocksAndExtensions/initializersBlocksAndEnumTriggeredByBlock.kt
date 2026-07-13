// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM

var initOrder = ""

enum class Foo(val f: String) {
    Alpha(run {
        initOrder += "1"
        "alpha"
    });

    companion {
        fun call() {}

        val blockProp: String = run {
            initOrder += "2"
            "block"
        }
    }
}

fun box(): String {
    // Trigger initialization of enum entries and static property by call static method
    val entries = Foo.entries
    if (initOrder != "12") return "FAIL: initOrder=$initOrder"

    val a = Foo.Alpha
    if (a.f != "alpha") return "FAIL: Alpha.f=${a.f}"

    val b = Foo.blockProp
    if (b != "block") return "FAIL: blockProp=$b"

    return "OK"
}
