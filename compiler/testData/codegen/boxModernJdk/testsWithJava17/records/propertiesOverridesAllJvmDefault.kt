// ENABLE_JVM_PREVIEW
// !JVM_DEFAULT_MODE: all

// FILE: JavaClass.java
public class JavaClass {
    public static String box() {
        MyRec m = new MyRec<String>("O", "K");
        KI<String> ki = m;
        return m.x() + m.y() + ki.getX() + ki.getY();
    }
}
// FILE: main.kt

interface KI<T> {
    val x: String get() = ""
    val y: T
}

@JvmRecord
data class MyRec<R>(override val x: String, override val y: R) : KI<R>

fun box(): String {
    val res = JavaClass.box()
    if (res != "OKOK") return "fail 1: $res"
    return "OK"
}
