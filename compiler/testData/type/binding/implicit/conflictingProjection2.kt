fun <T> getT(): T = null!!

val foo = getT<List<in List<Int>>>()
/*
psi: val foo = getT<List<in List<Int>>>()
type: List<in List<Int>>
    typeParameter: <out E> defined in kotlin.collections.List
    typeProjection: in List<Int>
    psi: val foo = getT<List<in List<Int>>>()
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: val foo = getT<List<in List<Int>>>()
        type: Int
*/