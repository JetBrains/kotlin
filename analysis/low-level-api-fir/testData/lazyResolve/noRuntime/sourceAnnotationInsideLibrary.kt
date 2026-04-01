// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ALLOW_KOTLIN_PACKAGE
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: JavaClass.java
import java.util.Map;

@kotlin.jvm.PurelyImplements("kotlin.collections.MutableMap")
public abstract class JavaClass implements Map<Integer, String> {
}

// MODULE: main(lib)
// FILE: PurelyImplements.kt
package kotlin.jvm

public annotation class PurelyImplements(val value: String)
// FILE: test.kt

fun usa<caret>ge() {
    JavaClass()
}
