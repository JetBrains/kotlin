// FILE: TestClass.java
import org.jetbrains.annotations.Nullable;
public class TestClass {
    public <T> T set(@Nullable String key, @Nullable T t) {
        return t;
    }
}

// FILE: main.kt
fun run() {
    val testClass = TestClass()
    // inferred as `set<Nothing>()`, return type is Nothing!
    testClass.<!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>set<!>("test", null)

    // Should not be unreachable
    run()
}
