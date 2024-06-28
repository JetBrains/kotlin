// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// WITH_REFLECT
// FILE: B.java
public class B {
    public final int c = 1;
    public void bb() {}
    public final int b = 2;
    public final int a = 10;
    public void aa() {}
}

// FILE: main.kt
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

class A {
    val c = 1
    fun bb() {}
    val b = 2
    val a = 10
    fun aa() {}
}

fun listMembers(kClass: KClass<*>): String {
    return kClass.members.joinToString(" | ") { member ->
        val prefix = when (member) {
            is KFunction -> "fun"
            is KProperty -> "val"
            else -> "wtf"
        }
        "$prefix ${member.name}"
    }
}

fun box(): String {
    val aMembers = listMembers(A::class)

    // After migration of reflection to K2 the order will be following:
    // "val c | val b | val a | fun bb | fun aa | fun equals | fun hashCode | fun toString"
    if (aMembers != "val a | val b | val c | fun aa | fun bb | fun equals | fun hashCode | fun toString") return "Fail A: $aMembers"

// Looks like property order is different on different JDK, so it's pointless to test it
//    val bMembers = listMembers(B::class)
//    if (bMembers != "fun aa | fun bb | val c | val b | val a | fun equals | fun hashCode | fun toString") return "Fail B: $bMembers"

    return "OK"
}
