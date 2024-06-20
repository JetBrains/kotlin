// ISSUE: KT-49420

<!CONFLICTING_OVERLOADS!>fun <A> topLevelFoo(arg: A?)<!> {}
<!CONFLICTING_OVERLOADS!>fun <B> topLevelFoo(arg: B)<!> {}
<!CONFLICTING_OVERLOADS!>fun <C> topLevelFoo(arg: C & Any)<!> {}

class Klass<T> {
    fun memberFoo(arg: T?) {}
    fun memberFoo(arg: T) {}
    fun memberFoo(arg: T & Any) {}
}

/* overloading behavior rationale */

fun <A> fooA(arg: A?): A {
    <!DEBUG_INFO_EXPRESSION_TYPE("A?")!>fooB(arg)<!>
    fooC(<!TYPE_MISMATCH("A & Any; A?")!>arg<!>)
    return null!!
}
fun <B> fooB(arg: B): B {
    <!DEBUG_INFO_EXPRESSION_TYPE("B & Any")!>fooA(arg)<!>
    fooC(<!TYPE_MISMATCH("B & Any; B")!>arg<!>)
    return null!!
}
fun <C> fooC(arg: C & Any): C {
    <!DEBUG_INFO_EXPRESSION_TYPE("C & Any")!>fooA(arg)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("C & Any")!>fooB(arg)<!>
    return null!!
}
// fooA can delegate to fooB, fooB can delegate to fooA => fooA & fooB can't be overloads
// fooA can't delegate to fooC, fooC can delegate to fooA => fooA & fooC can be overloads (fooC is more specific)
// fooB can't delegate to fooC, fooC can delegate to fooB => fooB & fooC can be overloads (fooC is more specific)

class RationaleKlass<T> {
    fun fooD(arg: T?) {
        fooE(<!TYPE_MISMATCH("T; T?")!>arg<!>)
        fooF(<!TYPE_MISMATCH("T & Any; T?")!>arg<!>)
    }
    fun fooE(arg: T) {
        fooD(arg)
        fooF(<!TYPE_MISMATCH("T & Any; T")!>arg<!>)
    }
    fun fooF(arg: T & Any) {
        fooD(arg)
        fooE(arg)
    }
}
// fooD can't delegate to fooE, fooE can delegate to fooD => fooD & fooE can be overloads (fooE is more specific)
// fooD can't delegate to fooF, fooF can delegate to fooD => fooD & fooF can be overloads (fooF is more specific)
// fooE can't delegate to fooF, fooF can delegate to fooE => fooE & fooF can be overloads (fooF is more specific)
