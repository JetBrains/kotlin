fun <T> getT(): T = null!!

val foo = getT<List<in Int>>()
/*
psi: val foo = getT<List<in Int>>()
type: List<in Int>
    typeParameter: <out E> defined in kotlin.collections.List
    typeProjection: in Int
    psi: val foo = getT<List<in Int>>()
    type: Int
*/