// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt
expect interface Base {
    fun foo(a: Int): String
    fun getName(): String
}

class DelegatedImpl(val foo: Base) : Base by foo

// MODULE: platform()()(common)
// FILE: BaseJava.java
public interface BaseJava {
    String foo(int a);
    String getName();
}

// FILE: JavaImpl.java
public class JavaImpl implements BaseJava {
    @Override
    public String foo(int a) {
        return "O";
    }
    @Override
    public String getName() {
        return "K";
    }
}

// FILE: main.kt
actual typealias Base = BaseJava

fun box(): String {
    val x = DelegatedImpl(JavaImpl())
    return x.foo(1) + x.name
}
