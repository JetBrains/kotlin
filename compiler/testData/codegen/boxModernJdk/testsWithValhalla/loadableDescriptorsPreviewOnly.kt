// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// Only JVM preview features are enabled here, not Valhalla value classes, so the Kotlin full value class below is an identity class,
// unlike the Java and JDK value classes.

// FILE: JavaVal.java
public value class JavaVal {
    public final int x;

    public JavaVal(int x) {
        this.x = x;
    }
}

// FILE: test.kt
import java.time.LocalDate

value class KotlinVal(val x: Int)

class Holder(val javaVal: JavaVal, val date: LocalDate, val boxed: Int?, val kotlinVal: KotlinVal) {
    fun withJavaVal(v: JavaVal?): LocalDate? = null
}

fun box(): String {
    val holder = Holder(JavaVal(1), LocalDate.MIN, 2, KotlinVal(3))
    if (holder.javaVal.x != 1 || holder.boxed != 2 || holder.kotlinVal.x != 3) return "Fail"
    return "OK"
}

// 0 ATTRIBUTE LoadableDescriptors
