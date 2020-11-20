class In<in T>
class Out<out T>
class Inv<T>
class X

fun f1(p: In<in X>) {}
fun f2(p: <!CONFLICTING_PROJECTION!>In<out X><!>) {}
fun f3(p: In<X>) {}

fun f4(p: Out<out X>) {}
fun f5(p: <!CONFLICTING_PROJECTION!>Out<in X><!>) {}
fun f6(p: Out<X>) {}

fun f6(p: Inv<X>) {}
fun f7(p: Inv<in X>) {}
fun f8(p: Inv<out X>) {}

fun f9(p: In<*>) {}
fun f10(p: Out<*>) {}
fun f11(p: Inv<*>) {}
