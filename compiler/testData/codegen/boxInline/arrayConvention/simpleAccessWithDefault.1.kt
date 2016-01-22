import test.*


fun box(): String {

    val z = 1;

    val p = z[2, { 3 }]
    if (p != 106) return "fail 1: $p"

    val captured = 3;
    z[2, { captured } ] = p
    if (res != 112) return "fail 2: $res"

    return "OK"
}