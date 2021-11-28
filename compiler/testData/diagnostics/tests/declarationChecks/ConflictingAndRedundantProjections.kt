// FIR_IDENTICAL
class In<in T>
class Out<out T>
class Inv<T>
class X

fun f1(p: In<<!REDUNDANT_PROJECTION!>in<!> X>) {}
fun f2(p: In<<!CONFLICTING_PROJECTION!>out<!> X>) {}
fun f3(p: In<X>) {}

fun f4(p: Out<<!REDUNDANT_PROJECTION!>out<!> X>) {}
fun f5(p: Out<<!CONFLICTING_PROJECTION!>in<!> X>) {}
fun f6(p: Out<X>) {}

fun f6(p: Inv<X>) {}
fun f7(p: Inv<in X>) {}
fun f8(p: Inv<out X>) {}

fun f9(p: In<*>) {}
fun f10(p: Out<*>) {}
fun f11(p: Inv<*>) {}
