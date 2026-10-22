// DUMP_IR_DIFFERENCE: JKLIB
//   JKlib tests compile against a real JDK, whose default methods (`forEach`, `stream`, `spliterator`, ...) add fake overrides.
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
