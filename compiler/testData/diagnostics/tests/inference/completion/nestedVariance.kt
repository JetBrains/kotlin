// DIAGNOSTICS: -UNUSED_PARAMETER

interface Base
class Derived : Base
class OtherDerived : Base

class Inv<T>
class Out<out O>
class In<in I>

class BiParam<out F, out S>

fun <K> id(arg: K) = arg

fun <D> createDoubleIn(arg: D): In<In<D>> = TODO()
fun <D> createDoubleInOut1(arg: D): In<In<Out<D>>> = TODO()
fun <D> createDoubleInOut2(arg: D): In<Out<In<D>>> = TODO()
fun <D> createDoubleInOut3(arg: D): Out<In<In<D>>> = TODO()

fun <D> createInOut(arg: D): In<Out<D>> = TODO()
fun <D> createOutIn(arg: D): Out<In<D>> = TODO()
fun <D> createInvOut(arg: D): Inv<Out<D>> = TODO()
fun <D> createOutInvDoubleIn(arg: D): Out<Inv<In<In<D>>>> = TODO()

fun <F, S> biparamEarly(first: F, second: S): BiParam<In<In<F>>, Out<S>> = TODO()
fun <F, S> biparamEarlyIrrelevantInv(first: F, second: S): BiParam<In<In<F>>, Inv<String>> = TODO()
fun <F, S> biparamEarlyStarProjection(first: F, second: S): BiParam<*, In<In<S>>> = TODO()

fun <F, S> biparamLateIn(first: F, second: S): BiParam<In<Out<F>>, S> = TODO()
fun <F, S> biparamLateInv(first: F, second: S): BiParam<Inv<Out<F>>, S> = TODO()

fun <T> take(biParam: BiParam<*, In<In<T>>>) {}

fun testEarlyCompletion(derived: Derived, otherDerived: OtherDerived) {
    id<In<In<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<In<Derived>>")!>createDoubleIn(derived)<!>
    )
    id<In<In<Out<Base>>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<In<Out<Derived>>>")!>createDoubleInOut1(derived)<!>
    )
    id<In<Out<In<Base>>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<Out<In<Derived>>>")!>createDoubleInOut2(derived)<!>
    )
    id<Out<In<In<Base>>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<In<In<Derived>>>")!>createDoubleInOut3(derived)<!>
    )
    id<BiParam<In<In<Base>>, Out<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<In<In<Derived>>, Out<OtherDerived>>")!>biparamEarly(derived, otherDerived)<!>
    )
    id<BiParam<In<In<Base>>, Inv<String>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<In<In<Derived>>, Inv<kotlin.String>>")!>biparamEarlyIrrelevantInv(derived, otherDerived)<!>
    )
    take<Base>(
        <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<*, In<In<OtherDerived>>>")!>biparamEarlyStarProjection(derived, otherDerived)<!>
    )
}

fun testLateCompletion(derived: Derived, otherDerived: OtherDerived) {
    id<In<Out<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<Out<Base>>")!>createInOut(derived)<!>
    )
    id<Out<In<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<In<Base>>")!>createOutIn(derived)<!>
    )
    id<Inv<Out<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Out<Base>>")!>createInvOut(derived)<!>
    )
    id<Out<Inv<In<In<Base>>>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<Inv<In<In<Base>>>>")!>createOutInvDoubleIn(derived)<!>
    )
    id<BiParam<In<Out<Base>>, OtherDerived>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<In<Out<Base>>, OtherDerived>")!>biparamLateIn(derived, otherDerived)<!>
    )
    id<BiParam<Inv<Out<Base>>, OtherDerived>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("BiParam<Inv<Out<Base>>, OtherDerived>")!>biparamLateInv(derived, otherDerived)<!>
    )
}
