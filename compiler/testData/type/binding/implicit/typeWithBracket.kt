interface Inv<I>

fun <T> getT(): T = null!!

val foo = getT<Inv<out ((Inv<Int>)?)>>()
/*
psi: val foo = getT<Inv<out ((Inv<Int>)?)>>()
type: Inv<out Inv<Int>?>
    typeParameter: <I> defined in Inv
    typeProjection: out Inv<Int>?
    psi: val foo = getT<Inv<out ((Inv<Int>)?)>>()
    type: Inv<Int>?
        typeParameter: <I> defined in Inv
        typeProjection: Int
        psi: val foo = getT<Inv<out ((Inv<Int>)?)>>()
        type: Int
*/