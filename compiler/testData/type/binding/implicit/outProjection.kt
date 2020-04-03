interface Inv<T>
interface In<in I>
interface Out<out O>


fun <T> getT(): T = null!!

val foo = getT<Inv<out In<out Out<out Int>>>>()
/*
psi: val foo = getT<Inv<out In<out Out<out Int>>>>()
type: Inv<out [ERROR : Inconsistent type: In<out Out<out Int>> (0 parameter has declared variance: in, but argument variance is out)]>
    typeParameter: <T> defined in Inv
    typeProjection: out [ERROR : Inconsistent type: In<out Out<out Int>> (0 parameter has declared variance: in, but argument variance is out)]
    psi: val foo = getT<Inv<out In<out Out<out Int>>>>()
    type: [ERROR : Inconsistent type: In<out Out<out Int>> (0 parameter has declared variance: in, but argument variance is out)]
*/