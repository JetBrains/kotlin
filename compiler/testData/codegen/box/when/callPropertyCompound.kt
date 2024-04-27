class C(val p: Boolean) { }

fun box(): String {


    return if (val c = C(true); c.p) "OK" else "fail"
}
