// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63853, KT-63854

// MODULE: lib
// FILE: NameHighlighter.kt

object NameHighlighter {
    var namesHighlightingEnabled = true
        @TestOnly set
}

// FILE: TestOnly.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface TestOnly {
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    if (!NameHighlighter.namesHighlightingEnabled) return "FAIL 1"
    NameHighlighter.namesHighlightingEnabled = false
    if (NameHighlighter.namesHighlightingEnabled) return "FAIL 2"
    return "OK"
}
