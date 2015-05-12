interface Inv<T>
interface In<in I>
interface Out<out O>


fun <T> getT(): T = null!!

val foo = getT<Inv<in In<in Out<in Int>>>>()
/*
psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
type: Inv<in In<in Out<in Int>>>
    typeParameter: <T> defined in Inv
    typeProjection: in In<in Out<in Int>>
    psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
    type: In<in Out<in Int>>
        typeParameter: <in I> defined in In
        typeProjection: in Out<in Int>
        psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
        type: Out<in Int>
            typeParameter: <out O> defined in Out
            typeProjection: in Int
            psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
            type: Int
*/