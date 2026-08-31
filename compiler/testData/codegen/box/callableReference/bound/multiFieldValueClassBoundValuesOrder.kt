// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

class Host(val label: String) {
    fun bind(point: Point): () -> String {
        fun render(): String = "HP:$label:${point.x}:${point.y}"
        return ::render
    }
}

value class Point(val x: Int, val y: Int) {
    fun bind(host: Host): () -> String {
        fun render(): String = "PH:$x:$y:${host.label}"
        return ::render
    }
}

fun box(): String {
    val host = Host("host")
    val point = Point(1, 2)

    val hostThenPoint = host.bind(point)
    val pointThenHost = point.bind(host)

    val first = hostThenPoint()
    if (first != "HP:host:1:2") return "FAIL host/point order"

    val second = pointThenHost()
    if (second != "PH:1:2:host") return "FAIL point/host order"

    return "OK"
}
