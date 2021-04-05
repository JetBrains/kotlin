// SOURCE_RETENTION_ANNOTATIONS
// FILE: BaseClass.java
import org.checkerframework.checker.nullness.qual.*;

public class BaseClass {
    public void loadCache(@Nullable Object... args) {}
}

// FILE: main.kt
class A : BaseClass() {
    override fun loadCache(vararg args: Any?) {
        super.loadCache(*args)
    }
}

class B : BaseClass() {
    // org.checkerframework.checker.nullness.qual.Nullable has @Target TYPE_USE, so it affects only elements type
    <!NOTHING_TO_OVERRIDE!>override<!> fun loadCache(vararg args: Any) {
        super.loadCache(*args)
    }
}
