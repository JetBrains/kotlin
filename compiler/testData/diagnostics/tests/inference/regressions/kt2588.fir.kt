// !DIAGNOSTICS: -UNUSED_VARIABLE
//T-2588 Allow to specify exact super type (expected) in inference if many

import java.util.HashSet

class MyClass<T>()

interface A
interface D
class B : A, D
class C : A, D

fun <T> hashSetOf(vararg values: T): HashSet<T> = throw Exception("$values")

fun foo(b: MyClass<B>, c: MyClass<C>) {
    val set1 : Set<MyClass<out D>> = hashSetOf(b, c) //type inference expected type mismatch
    val set2  = hashSetOf(b, c) //Set<MyClass<out Any>> is inferred
}