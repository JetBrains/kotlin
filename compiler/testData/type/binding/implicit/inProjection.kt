interface Inv<T>
interface In<in I>
interface Out<out O>


fun <T> getT(): T = null!!

val foo = getT<Inv<in In<in Out<in Int>>>>()
/*
psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
type: Inv<in In<[ERROR : Inconsistent type: Out<in Int> (0 parameter has declared variance: out, but argument variance is in)]>>
    typeParameter: <T> defined in Inv
    typeProjection: in In<[ERROR : Inconsistent type: Out<in Int> (0 parameter has declared variance: out, but argument variance is in)]>
    psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
    type: In<[ERROR : Inconsistent type: Out<in Int> (0 parameter has declared variance: out, but argument variance is in)]>
        typeParameter: <in I> defined in In
        typeProjection: [ERROR : Inconsistent type: Out<in Int> (0 parameter has declared variance: out, but argument variance is in)]
        psi: val foo = getT<Inv<in In<in Out<in Int>>>>()
        type: [ERROR : Inconsistent type: Out<in Int> (0 parameter has declared variance: out, but argument variance is in)]
*/