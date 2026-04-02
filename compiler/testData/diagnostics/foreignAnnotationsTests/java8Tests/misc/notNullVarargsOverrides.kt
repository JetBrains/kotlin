// SOURCE_RETENTION_ANNOTATIONS
// FILE: BaseClass.java
import org.checkerframework.checker.nullness.qual.*;

public class BaseClass {
    public void loadCache(@NonNull Object... args) {}
}

// FILE: main.kt
class A : BaseClass() {
    // org.checkerframework.checker.nullness.qual.NonNull has @Target TYPE_USE, so it affects only elements type
    <!NOTHING_TO_OVERRIDE!>override<!> fun loadCache(vararg args: Any?) {
        super.loadCache(*<!ARGUMENT_TYPE_MISMATCH!>args<!>)
    }
}

class B : BaseClass() {
    override fun loadCache(vararg args: Any) {
        super.loadCache(*args)
    }
}
