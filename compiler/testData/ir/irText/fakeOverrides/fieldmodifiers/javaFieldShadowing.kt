// SKIP_KT_DUMP
// SKIP_IR_DESERIALIZATION_CHECKS

// FILE: BaseJava.java
public class BaseJava {
    public String foo = "abc";
}

// FILE: JavaSub.java
public class JavaSub extends BaseJava {
    public String foo = "ijk";
}

// FILE: main.kt
class KtSub : JavaSub()

fun test(j: JavaSub, k: KtSub) {
    val a = j.foo
    val b = k.foo
}
