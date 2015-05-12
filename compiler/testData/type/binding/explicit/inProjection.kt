interface Inv<T>
interface In<in I>
interface Out<out O>


val foo: Inv<in In<in Out<in Int>>> = null!!
/*
psi: Inv<in In<in Out<in Int>>>
type: Inv<in In<in Out<in Int>>>
    typeParameter: <T> defined in Inv
    typeProjection: in In<in Out<in Int>>
    psi: In<in Out<in Int>>
    type: In<in Out<in Int>>
        typeParameter: <in I> defined in In
        typeProjection: in Out<in Int>
        psi: Out<in Int>
        type: Out<in Int>
            typeParameter: <out O> defined in Out
            typeProjection: in Int
            psi: Int
            type: Int
*/