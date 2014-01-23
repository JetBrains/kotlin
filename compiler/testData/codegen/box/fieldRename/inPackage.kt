class A { }
class B { }

public val A.prop: Int = 0;
public val B.prop: String = "1111";
public val prop: Double = 0.1;

public fun doTest() : String {
    if (A().prop != 0) return "fail1"
    if (B().prop != "1111") return "fail2"
    if (prop != 0.1) return "fail3"

    return "OK"
}

fun box() : String {
  return doTest()
}
