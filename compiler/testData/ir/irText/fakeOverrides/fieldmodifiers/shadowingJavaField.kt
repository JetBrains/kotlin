// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// SKIP_IR_DESERIALIZATION_CHECKS
// REASON: Deserialization synthesizes fake overrides for Java superclass fields (BaseJava.foo) during KLIB linking.

// FILE: BaseJava.java
public class BaseJava {
    public String foo = "java_base_foo";
}

// FILE: 1.kt
import kotlin.jvm.JvmField

class KtSub : BaseJava() {
    @JvmField
    var foo: String = "kt_sub_foo"
}

fun test(sub: KtSub) {
    sub.foo
}
