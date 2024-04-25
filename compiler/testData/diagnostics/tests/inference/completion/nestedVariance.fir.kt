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
        createDoubleIn(derived)
    )
    id<In<In<Out<Base>>>>(
        createDoubleInOut1(derived)
    )
    id<In<Out<In<Base>>>>(
        createDoubleInOut2(derived)
    )
    id<Out<In<In<Base>>>>(
        createDoubleInOut3(derived)
    )
    id<BiParam<In<In<Base>>, Out<Base>>>(
        biparamEarly(derived, otherDerived)
    )
    id<BiParam<In<In<Base>>, Inv<String>>>(
        biparamEarlyIrrelevantInv(derived, otherDerived)
    )
    take<Base>(
        biparamEarlyStarProjection(derived, otherDerived)
    )
}

fun testLateCompletion(derived: Derived, otherDerived: OtherDerived) {
    id<In<Out<Base>>>(
        createInOut(derived)
    )
    id<Out<In<Base>>>(
        createOutIn(derived)
    )
    id<Inv<Out<Base>>>(
        createInvOut(derived)
    )
    id<Out<Inv<In<In<Base>>>>>(
        createOutInvDoubleIn(derived)
    )
    id<BiParam<In<Out<Base>>, OtherDerived>>(
        biparamLateIn(derived, otherDerived)
    )
    id<BiParam<Inv<Out<Base>>, OtherDerived>>(
        biparamLateInv(derived, otherDerived)
    )
}
