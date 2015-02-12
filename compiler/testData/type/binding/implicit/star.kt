fun <T> getT(): T = null!!

val foo = getT<List<*>>()
/*
psi: val foo = getT<List<*>>()
type: List<*>
    typeParameter: <out E> defined in kotlin.List
    typeProjection: *
    psi: val foo = getT<List<*>>()
    type: Any?
*/