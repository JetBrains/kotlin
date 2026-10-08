// ISSUE: KT-89989
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

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

// 1 ATTRIBUTE LoadableDescriptors : LJavaVal;, Ljava/time/LocalDate;, Ljava/lang/Integer;\n
