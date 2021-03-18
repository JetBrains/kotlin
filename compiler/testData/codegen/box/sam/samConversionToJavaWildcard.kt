// TARGET_BACKEND: JVM
// FULL_JDK
// FILE: ConventionMapping.java
import java.util.concurrent.Callable;

public class ConventionMapping {
    MappedProperty map(String propertyName, Callable<?> value) {
        return new MappedProperty();
    }

    public static class MappedProperty {

    }
}

// FILE: FileCollection.java

public class FileCollection {}

// FILE: test.kt

fun test(mapping: ConventionMapping, fn: () -> FileCollection) {
    mapping.map("classpath", fn)
}

fun box(): String = "OK"
