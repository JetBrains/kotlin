// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49420

<!CONFLICTING_OVERLOADS!>fun <A> topLevelFoo(arg: A?)<!> {}
<!CONFLICTING_OVERLOADS!>fun <B> topLevelFoo(arg: B)<!> {}
fun <C> topLevelFoo(arg: C & Any) {}

class Klass<T> {
    fun memberFoo(arg: T?) {}
    fun memberFoo(arg: T) {}
    fun memberFoo(arg: T & Any) {}
}

/* overloading behavior rationale */

fun <A> fooA(arg: A?): A {
    <!DEBUG_INFO_EXPRESSION_TYPE("A?")!>fooB(arg)<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>fooC<!>(<!ARGUMENT_TYPE_MISMATCH("C (of fun <C> fooC) & Any; A? (of fun <A> fooA)")!>arg<!>)
    return null!!
}
fun <B> fooB(arg: B): B {
    <!DEBUG_INFO_EXPRESSION_TYPE("B & Any")!>fooA(arg)<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>fooC<!>(<!ARGUMENT_TYPE_MISMATCH("C (of fun <C> fooC) & Any; B (of fun <B> fooB)")!>arg<!>)
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
        fooE(<!ARGUMENT_TYPE_MISMATCH("T (of class RationaleKlass<T>); T? (of class RationaleKlass<T>)")!>arg<!>)
        fooF(<!ARGUMENT_TYPE_MISMATCH("T (of class RationaleKlass<T>) & Any; T? (of class RationaleKlass<T>)")!>arg<!>)
    }
    fun fooE(arg: T) {
        fooD(arg)
        fooF(<!ARGUMENT_TYPE_MISMATCH("T (of class RationaleKlass<T>) & Any; T (of class RationaleKlass<T>)")!>arg<!>)
    }
    fun fooF(arg: T & Any) {
        fooD(arg)
        fooE(arg)
    }
}
// fooD can't delegate to fooE, fooE can delegate to fooD => fooD & fooE can be overloads (fooE is more specific)
// fooD can't delegate to fooF, fooF can delegate to fooD => fooD & fooF can be overloads (fooF is more specific)
// fooE can't delegate to fooF, fooF can delegate to fooE => fooE & fooF can be overloads (fooF is more specific)
