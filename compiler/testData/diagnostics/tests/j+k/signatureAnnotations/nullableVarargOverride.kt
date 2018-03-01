// FILE: BaseClass.java
import org.jetbrains.annotations.Nullable;

public class BaseClass {
    public void loadCache(@Nullable Object... args) {}
}

// FILE: main.kt

class A : BaseClass() {
    // org.jetbrains.annotations.Nullable has @Target PARAMETER, so it doesn't affect elements type
    override fun loadCache(vararg args: Any?) {
        super.loadCache(*args)
    }
}

class B : BaseClass() {
    // org.jetbrains.annotations.Nullable has @Target PARAMETER, so it doesn't affect elements type
    override fun loadCache(vararg args: Any) {
        super.loadCache(*args)
    }
}
