import javaClassLiteral as J

fun box(): String {
    val j = J::class
    if (j.simpleName != "javaClassLiteral") return "Fail: ${j.simpleName}"

    return "OK"
}
