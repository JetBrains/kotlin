// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// WITH_STDLIB
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: boundRefToSuperInterfaceMethod.kt

class Impl(val set1: Set<String>, val set2: Set<String>) : JDerived {
    override fun is1(x: String) = x in set1
    override fun is2(x: String) = x in set2
}

fun cmp(d: JDerived) =
    Comparator.comparing(d::is1)
        .thenComparing(d::is2)

fun box(): String {
    val cmp = cmp(Impl(setOf("a", "c"), setOf("c", "d")))
    val list = listOf("e", "d", "c", "b", "a").sortedWith(cmp)
    if (list != listOf("e", "b", "d", "a", "c"))
        return "Failed: ${list.toString()}"
    return "OK"
} 

// FILE: JBase.java

public interface JBase {
    boolean is1(String x);
}

// FILE: JDerived.java

public interface JDerived extends JBase {
    boolean is2(String x);
}
