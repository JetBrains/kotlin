fun box(): String {
    val l = java.util.ArrayList<Int>()
    l.add(1000)

    val x = l[0] === 1000
    if (x) return "Fail: $x"
    val x1 = l[0] === 1
    if (x1) return "Fail 1: $x"
    val x2 = l[0] === l[0]
    if (!x2) return "Fail 2: $x"

    val y = l[0] identityEquals 1000
    if (y) return "Fail (y): $y"
    val y1 = l[0] identityEquals 1
    if (y1) return "Fail (y1): $y"
    val y2 = l[0] identityEquals l[0]
    if (!y2) return "Fail (y2): $y"

    return "OK"
}