// FILE: A.java

public class A {}

// FILE: Wrapper.java

public abstract class Wrapper<T extends A> {
    protected T t;

    Wrapper(T t) { this.t = t; }
}

// FILE: kt7585.kt

class E

class MyWrapper(a: A): Wrapper<A>(a)

// This wrapper is not legal
class TheirWrapper(e: E): Wrapper<<!UPPER_BOUND_VIOLATED!>E<!>>(e)

data class Pair<out T>(val a: T, val b: T)

fun foo(): String {
    val matrix: Pair<Wrapper<*>>
    // It's not legal to do such a thing because E is not derived from A
    // But we should not have assertion errors because of it!
    matrix = Pair(MyWrapper(A()), TheirWrapper(E()))
    return matrix.toString()
}
