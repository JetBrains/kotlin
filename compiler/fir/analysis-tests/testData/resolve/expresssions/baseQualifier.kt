// FILE: JavaClass.java

public class JavaClass {
    public static void bar() {}
}

// FILE: Test.kt

open class AA : JavaClass() {
    object C
}

class BB : AA() {
    object D
}

fun test() {
    val bbd = BB.D
    val aac = AA.C
    JavaClass.bar()

    val errC = BB.<!UNRESOLVED_REFERENCE!>C<!>
    val errBarViaBB = BB.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
    val errBarViaAA = AA.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>bar<!>()<!>
}
