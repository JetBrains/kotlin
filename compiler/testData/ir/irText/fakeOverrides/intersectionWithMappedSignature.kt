// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// FILE: Java1.java
public interface Java1 {
    Boolean remove(Integer element);
}

// FILE: 1.kt
import java.util.*;

abstract class B : ArrayList<Int>(), Java1 {
}