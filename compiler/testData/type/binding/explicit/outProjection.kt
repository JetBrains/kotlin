interface Inv<T>
interface In<in I>
interface Out<out O>


val foo: Inv<out In<out Out<out Int>>> = null!!
/*
psi: Inv<out In<out Out<out Int>>>
type: Inv<out In<out Out<out Int>>>
    typeParameter: <T> defined in Inv
    typeProjection: out In<out Out<out Int>>
    psi: In<out Out<out Int>>
    type: In<out Out<out Int>>
        typeParameter: <in I> defined in In
        typeProjection: out Out<out Int>
        psi: Out<out Int>
        type: Out<out Int>
            typeParameter: <out O> defined in Out
            typeProjection: out Int
            psi: Int
            type: Int
*/