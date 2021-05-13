// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// We've already had errors in source mode, so it's relevant only for binaries for now

// FILE: Base.java
// INCLUDE_JAVA_AS_BINARY
public class Base<K> {}

// FILE: Test.java
// INCLUDE_JAVA_AS_BINARY
import org.jetbrains.annotations.Nullable;

class Test extends Base<@Nullable String> {}

// FILE: main.kt
fun takeBaseOfNotNullStrings(x: Base<String>) {}

fun main() {
    val x = takeBaseOfNotNullStrings(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>Test()<!>)
}