// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -PCLAEnhancementsIn21

class Container<T> {
    fun produce(): T = TODO()
}

class TypePair<L, R> // no variance

fun <A, B: A> func(lambda: (Container<A>) -> (Container<B>) -> Unit): TypePair<A, B> = TODO()
fun <T> consume(arg: T) {}

open class Parent
open class Child: Parent()

fun main() {
    val result = func /* Bv <: Av */ (
        { containerA/*: Container<Av> */ ->
            { <!CANNOT_INFER_PARAMETER_TYPE!>containerB<!>/*: Container<Bv> */ ->
                consume<Parent>(containerA.produce()) // Av <: Parent
                consume<Child>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>containerB<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>produce<!>()) // Bv <: Child
            }
        }
        // resulting system of constraints:
        // Bv <: Av <: Parent
        //    <: Child
        // resulting type arguments: Av = Parent, Bv = Child
    )
    // resulting expression type: TypePair<Parent, Child>
    val test: TypePair<Parent, Child> = <!TYPE_MISMATCH!>result<!>
}
