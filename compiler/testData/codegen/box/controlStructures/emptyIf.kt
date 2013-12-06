fun box(): String {
    if (true);
    if (false);
    val iftrue = if (true);
    val iffalse = if (false);

    var state = 0
    val k = if (state++==1);
    if (state != 1) return "Fail: $state"

    return "OK"
}
