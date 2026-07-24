// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK

// FILE: Util.java
public class Util {
    public static String getString() { return null; }
}

// FILE: PrivateInterface.kt
package foo

interface PublicInterface {
    fun foo() = 10
}

private interface PrivateInterface : PublicInterface

open class A : PrivateInterface
open class B : PrivateInterface

// FILE: main.kt
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.*
import foo.*

fun testWithMap(map: ConcurrentHashMap<Int, String>): Int {
    var string = map[1]
    if (string == null) {
        string = map.computeIfAbsent(1) { "hello" }
    }
    return string.length
}

fun testWithUtil(map: ConcurrentHashMap<Int, String>): Int {
    var string = map[1]
    if (string == null) {
        string = Util.getString()
    }
    return string.length
}

fun test(list: java.util.ArrayList<String?>) {
    val x = list.get(0)<!UNSAFE_CALL!>.<!>length
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T> hihiEquals(flexible: T, other: Any?): Boolean {
    contract {
        returns(true) implies (other is T)
    }
    return other is T
}

inline fun <reified P1 : <!EXPOSED_TYPE_PARAMETER_BOUND, INVISIBLE_REFERENCE, LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PrivateInterface<!>, reified P2 : P1> testTypeParameter(p1: P1, p2: P2): Int {
    var string: String? = ""
    if (!(hihiEquals(p1, string) || hihiEquals(p2, string))) {
        return 0
    }
    return string.<!UNRESOLVED_REFERENCE!>foo<!>()
}

inline fun <reified P1 : <!EXPOSED_TYPE_PARAMETER_BOUND!><!INVISIBLE_REFERENCE, LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>PrivateInterface<!>?<!>, reified P2 : P1> testDnn(p1: P1 & Any, p2: P2 & Any): Int {
    var string: String? = ""
    if (!(hihiEquals(p1, string) || hihiEquals(p2, string))) {
        return 0
    }
    return string.<!UNRESOLVED_REFERENCE!>foo<!>()
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, flexibleType, functionDeclaration, ifExpression, inProjection,
integerLiteral, javaFunction, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration,
samConversion, smartcast, stringLiteral */
