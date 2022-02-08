// WITH_STDLIB
// FILE: test.kt

import java.util.Collections

class PlatformTypes {
    fun simplyPlatform() = Collections.singletonList("")[0]
    fun bothNullable() = Collections.emptyList<String>() ?: null
    fun bothNotNull() = Collections.emptyList<String>()!!

    fun <R> generic(j: J<R>) = j.get()
}

// FILE: J.java

public interface J<R> {
    R get();
}
