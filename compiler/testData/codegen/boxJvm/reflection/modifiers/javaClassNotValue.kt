// TARGET_BACKEND: JVM
// WITH_REFLECT

// FILE: JavaPlain.java
public final class JavaPlain {
    public final int x;
    public JavaPlain(int x) { this.x = x; }
    public int doubled() { return x * 2; }
    @Override public String toString() { return "JavaPlain(" + x + ")"; }
}

// FILE: box.kt
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

fun box(): String {
    if (JavaPlain::class.isValue) return "FAIL: JavaPlain::class.isValue should be false"

    val v = JavaPlain(42)

    val toStringFun = JavaPlain::class.functions.first { it.name == "toString" && it.parameters.size == 1 }
    if (toStringFun.call(v) != "JavaPlain(42)") return "FAIL toString via call: ${toStringFun.call(v)}"

    val doubled = JavaPlain::class.functions.first { it.name == "doubled" }
    if (doubled.call(v) != 84) return "FAIL doubled via call: ${doubled.call(v)}"
    if (doubled.callBy(mapOf(doubled.instanceParameter!! to v)) != 84) return "FAIL doubled via callBy"

    return "OK"
}
