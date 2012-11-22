class In<in T>
class Out<out T>
class Inv<T>
class X

fun f1(<!UNUSED_PARAMETER!>p<!>: In<<!REDUNDANT_PROJECTION!>in<!> X>) {}
fun f2(<!UNUSED_PARAMETER!>p<!>: In<<!CONFLICTING_PROJECTION!>out<!> X>) {}
fun f3(<!UNUSED_PARAMETER!>p<!>: In<X>) {}

fun f4(<!UNUSED_PARAMETER!>p<!>: Out<<!REDUNDANT_PROJECTION!>out<!> X>) {}
fun f5(<!UNUSED_PARAMETER!>p<!>: Out<<!CONFLICTING_PROJECTION!>in<!> X>) {}
fun f6(<!UNUSED_PARAMETER!>p<!>: Out<X>) {}

fun f6(<!UNUSED_PARAMETER!>p<!>: Inv<X>) {}
fun f7(<!UNUSED_PARAMETER!>p<!>: Inv<in X>) {}
fun f8(<!UNUSED_PARAMETER!>p<!>: Inv<out X>) {}

fun f9(<!UNUSED_PARAMETER!>p<!>: In<*>) {}
fun f10(<!UNUSED_PARAMETER!>p<!>: Out<*>) {}
fun f11(<!UNUSED_PARAMETER!>p<!>: Inv<*>) {}
