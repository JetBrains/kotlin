// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// JVM_EXPOSE_BOXED

// FILE: IC.kt
@JvmInline
value class StringWrapper(val s: String?)

class Test(val w: StringWrapper?, val notNull: String)

// FILE: Main.java
public class Main {
    public String viaExposedConstructor() {
        try {
            new Test(new StringWrapper("OK"), null);
        } catch (NullPointerException e) {
            return e.getMessage();
        }
        return "no exception";
    }
}

// FILE: Box.kt
import java.lang.reflect.InvocationTargetException

private fun viaAccessor(): String {
    val marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
    val accessor = Test::class.java.getDeclaredConstructor(StringWrapper::class.java, String::class.java, marker)
    accessor.isAccessible = true
    val boxed: StringWrapper? = StringWrapper("OK")
    try {
        accessor.newInstance(boxed, null, null)
    } catch (e: InvocationTargetException) {
        val cause = e.cause
        if (cause !is NullPointerException) return "not an NPE: $cause"
        return cause.message ?: "no message"
    }
    return "no exception"
}

fun box(): String {
    val fromJava = Main().viaExposedConstructor()
    if (!fromJava.startsWith("Parameter specified as non-null is null")) return "FAIL exposed: $fromJava"

    val fromAccessor = viaAccessor()
    if (!fromAccessor.startsWith("Parameter specified as non-null is null")) return "FAIL accessor: $fromAccessor"

    return "OK"
}
