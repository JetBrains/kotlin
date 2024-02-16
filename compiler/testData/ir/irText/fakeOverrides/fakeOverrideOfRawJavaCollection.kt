// ISSUE: KT-65448
// TARGET_BACKEND: JVM
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// FILE: Java1.java
import java.util.ArrayList;

public abstract class Java1 extends ArrayList { }

// FILE: 1.kt
abstract class E : Java1(){
}