// RUN_PIPELINE_TILL: FRONTEND
class Container<T> {
    fun produce(): T = null!!
}

class TypePair<L, R> // no variance

fun <A, B: A> func(lambda: (Container<A>) -> (Container<B>) -> Unit): TypePair<A, B> = null!!

fun <T> consume(arg: T) {}

open class Parent
open class Child: Parent()

fun main() {
    func /* Bv <: Av */ { containerA/*: Container<Av> */ ->
        consume<Parent>(containerA.produce()) // Av <: Parent
        // PCLA mode: analysis of lambdas in return positions is performed immediately
        return@func { containerB/*: Container<Bv> */ ->
            // ok
            consume<Child>(<!TYPE_MISMATCH!>containerB.produce()<!>) // Bv <: Child
        }
        // resulting system of constraints:
        // Bv <: Av <: Parent
        //    <: Child
        // resulting type arguments: Av = Parent, Bv = Child
    }
    // resulting expression type: TypePair<Parent, Child>

    func<Parent, _> /* Bv <: Parent */ { containerA/*: Container<Parent> */ ->
        consume<Parent>(containerA.produce())
        // standard inference mode: analysis of lambdas in return positions is postponed
        // resulting system of constraints:
        // Bv <: Parent
        // resulting type arguments: Bv = Parent
        return@func { containerB/*: Container<Parent> */ ->
            // ARGUMENT_TYPE_MISMATCH: Parent </: Child
            consume<Child>(<!TYPE_MISMATCH!>containerB.produce()<!>)
        }
    }
    // resulting expression type: TypePair<Parent, Parent>
}
