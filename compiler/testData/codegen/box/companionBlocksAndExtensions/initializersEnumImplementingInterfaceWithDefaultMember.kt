// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM_IR, NATIVE

var initOrder = ""

interface Marker {
    companion {
        val markerProp: String = run { initOrder += "I"; "marker" }
    }

    // A non-abstract (default) member. Per KEEP §3.3, an implementing classifier's
    // initialization must also trigger this interface's companion block initialization.
    fun describe(): String = "described"

    fun tag(): String
}

enum class Foo(val f: String) : Marker {
    Alpha(run { initOrder += "1"; "alpha" });

    override fun tag() = "Foo"

    companion {
        val blockProp: String = run { initOrder += "2"; "block" }
    }
}

fun box(): String {
    // Accessing the enum entry initializes the enum, which must first initialize the
    // companion block of its superinterface `Marker` (it has a non-abstract member).
    val a = Foo.Alpha
    if (a.f != "alpha") return "FAIL: Alpha.f=${a.f}"
    if (a.tag() != "Foo") return "FAIL: tag=${a.tag()}"
    if (a.describe() != "described") return "FAIL: describe=${a.describe()}"

    // §3.3: a superinterface with a non-abstract member is initialized before the
    // implementing classifier. §3.2.2: then enum entries, then companion blocks.
    if (initOrder != "I12") return "FAIL: initOrder after enum init=$initOrder"

    // Accessing companion members must not re-run any initializer.
    if (Foo.blockProp != "block") return "FAIL: blockProp=${Foo.blockProp}"
    if (Marker.markerProp != "marker") return "FAIL: markerProp=${Marker.markerProp}"
    if (initOrder != "I12") return "FAIL: initOrder final=$initOrder"

    return "OK"
}
