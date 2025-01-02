// RUN_PIPELINE_TILL: FRONTEND

// Case 1
interface I1<T> {
    fun foo(t: T)
}

open class A1 {
    open fun foo(o: Any) {}
}

class B1 : A1(), I1<Int> {
    override fun <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t: Int) {}
    // Synthetic java bridge method: override fun foo(t: Any) { foo((Int)t) } which overrides A1.foo
}

// Case 2

interface I2<T> {
    fun foo(t: T)
}

open class A2 {
    open fun<T> foo(o: T) {}
    // Type erased function open fun foo(o: Any)
}

class B2 : A2(), I2<Int> {
    override fun <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t: Int) {}
    // Synthetic java bridge method: override fun foo(t: Any) { foo((Int)t) } which overrides A2.foo
}

// Case 3

interface I3<T> {
    fun foo(t: T)
}

open class A3<T> {
    open fun foo(o: T) {}
    // Type erased function open fun foo(o: Any)
}

class B3 : A3<Any>(), I3<Int> {
    override fun <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t: Int) {}
    // Synthetic java bridge method: override fun foo(t: Any) { foo((Int)t) } which overrides A3.foo
}

// Case 4

open class D4a {}
class D4b: D4a() {}

interface I4a<T> {
    fun foo(t: T)
}

interface I4b<T> {
    fun foo(t: T)
}

open class A: I4b<Any> {
    override fun foo(t: Any) {}
    // foo(Any) -> foo((D1)o)
}

class B4 : A(), I4a<D4b> {
    override fun <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t: D4b) {}
    // Synthetic java bridge method: override fun foo(t: Any) { foo((D4b)t) } which overrides A4.foo
}

// Case 5

open class A5<T> {
    open fun foo(t: T) {}
}

class B5 : A5<Int>() {
    override fun foo(t: Int) {}
}

// Case 6

interface I6<T1> {
    fun<T2> foo(t1: T1, t2: T2)
}

open class A6 {
    open fun<T2> foo(t1: Any, t2: T2) {}
}

class B6 : A6(), I6<Int> {
    override fun<T> <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t1: Int, t2: T) {}
    // Synthetic java bridge method: override fun foo(t1: Any, t2:Any) { foo((Int)t1, ((Int)t2)) } which overrides A6.foo
}

// Case 7
interface I7<T1, T2> {
    fun foo(t1: T1, t2: T2)
}

open class A7<T1> {
    inner open class A7a<T2> {
        fun foo(t1: T1, t2: T2) {}
    }
}

class B7: A7<Any>() {
    inner class B7a : A7a<Any>(), I7<Int, Int> {
        override fun <!INHERITED_FUNCTION_NAME_CLASH_WITH_BRIDGE_METHOD!>foo<!>(t1: Int, t2: Int) {}
        // Synthetic java bridge method: override fun foo(t1: Any, t2:Any) { foo((Int)t1, ((Int)t2)) } which overrides A7a.foo
    }
}
