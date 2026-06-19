// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM_IR

var initOrder = ""

interface Marker {
    companion {
        val markerProp: String = run { initOrder += "I"; "marker" }
    }

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
    // Implementing an interface must not trigger the interface companion block init.
    // Accessing an enum entry triggers the enum's own initialization only.
    val a = Foo.Alpha
    if (a.f != "alpha") return "FAIL: Alpha.f=${a.f}"
    if (a.tag() != "Foo") return "FAIL: tag=${a.tag()}"

    // §3.2.2: enum entries are initialized first, then companion blocks follow
    // program order. The interface companion block is independent and must not
    // be initialized yet, as it has only abstract members.
    if (initOrder != "12") return "FAIL: initOrder after enum init=$initOrder"

    // Accessing the enum companion member must not re-run initializers.
    if (Foo.blockProp != "block") return "FAIL: blockProp=${Foo.blockProp}"
    if (initOrder != "12") return "FAIL: initOrder after companion access=$initOrder"

    // The interface companion block initializes only when its members are accessed.
    if (Marker.markerProp != "marker") return "FAIL: markerProp=${Marker.markerProp}"
    if (initOrder != "12I") return "FAIL: initOrder after interface access=$initOrder"

    return "OK"
}
