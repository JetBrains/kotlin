// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

import org.jetbrains.annotations.*;

// FILE: MapLike.java
public interface MapLike<@org.jetbrains.annotations.NotNull K> {
    void put(K x);
}

// FILE: main.kt
fun test2(map : MapLike<Int>, x2: Int?) {
    map.put(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x2<!>)
}