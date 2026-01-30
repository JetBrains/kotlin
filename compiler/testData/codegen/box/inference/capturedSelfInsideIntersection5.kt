// TARGET_BACKEND: JVM_IR
// ISSUE: KT-74999
// WITH_STDLIB

interface SelfI<C : SelfI<C>>

class A<S : SelfI<S>> {
    fun <T : S> materialize(): T = Entity() as T
}

val aStar: A<*> = A()
val aEntity: A<Entity> = A()

interface Path : Traversable
val path = object : Path {}
interface Traversable

open class Entity : Traversable, SelfI<Entity>

fun box() = "OK".also {
    when {
        "".hashCode() > 0 -> path
        else -> aStar.materialize()
    } // Ok in K1 and 2.2.0

    // The further tests can be uncommented after we've fixed the issue.

//    when {
//        "".hashCode() > 0 -> path
//        else -> aEntity.materialize()
//    } // Ok in K1, CCE in 2.2.0: class Entity cannot be cast to class Path

//    val x1 = when {
//        "".hashCode() > 0 -> path
//        else -> aStar.materialize()
//    } // Ok in 2.2.0, CCE in K1: class Entity cannot be cast to class Path

//    val x2 = when {
//        "".hashCode() > 0 -> path
//        else -> aEntity.materialize()
//    } // CCE both in K1 and K2: class Entity cannot be cast to class Path
}
