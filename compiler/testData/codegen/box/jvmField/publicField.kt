// TARGET_BACKEND: JVM

// WITH_RUNTIME

class A {
    @JvmField public val field = "OK";

    companion object {
        @JvmField public val cfield = "OK";
    }
}

object Object {
    @JvmField public val field = "OK";
}


fun box(): String {
    var result = A().field

    if (result != "OK") return "fail 1: $result"
    if (A.cfield != "OK") return "fail 2: ${A.cfield}"
    if (Object.field != "OK") return "fail 3: ${Object.field}"

    return "OK"

}
