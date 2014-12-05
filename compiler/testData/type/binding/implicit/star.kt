fun <T> getT(): T = null!!

val foo = getT<List<*>>()
/*
psi: val foo = getT<List<*>>()
type: List<Any?>
    typeParameter: <out E> defined in kotlin.List
    typeProjection: Any?
    psi: val foo = getT<List<*>>()
    type: Any?
*/