// TARGET_BACKEND: JVM
// FULL_JDK
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// FILE: Java1.java
public interface Java1 {
    Boolean remove(Integer element);
}

// FILE: 1.kt
import java.util.*;

abstract class B : ArrayList<Int>(), Java1 {
}