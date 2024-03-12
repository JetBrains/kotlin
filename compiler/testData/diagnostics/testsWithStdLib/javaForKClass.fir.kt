// !CHECK_TYPE

// FILE: A.java
import kotlin.reflect.KClass;

public class A {
    public static A getA() {
        return null;
    }

    public static KClass<A> getKClass() {
        return null;
    }
}


// types checked by txt file

// FILE: 1.kt
inline fun <reified X> test1() = X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
inline fun <reified X : Any> test2() = X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
inline fun <reified X : Any?> test3() = X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
inline fun <reified X : Number> test4() = X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
inline fun <reified X : Number?> test5() = X::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

fun test6() = A.getA()::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
fun test7() = A.getKClass().<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

