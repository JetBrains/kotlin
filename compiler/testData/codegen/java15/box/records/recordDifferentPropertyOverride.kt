// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW
// FILE: MyRec.java
public record MyRec(String name) implements KI {
    public String getName() {
        return "OK";
    }
}

// FILE: main.kt

interface KI {
    val name: String
}

fun box(): String {
    val r = MyRec("fail")
    if (r.name() != "fail") return "fail 1"
    if ((r as KI).name != "OK") return "fail 2"

    return r.name
}
