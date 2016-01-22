fun <T> getT(): T = null!!

val foo = getT<Pair<List<Int>, String>>()
/*
psi: val foo = getT<Pair<List<Int>, String>>()
type: Pair<List<Int>, String>
    typeParameter: <out A> defined in kotlin.Pair
    typeProjection: List<Int>
    psi: val foo = getT<Pair<List<Int>, String>>()
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: val foo = getT<Pair<List<Int>, String>>()
        type: Int

    typeParameter: <out B> defined in kotlin.Pair
    typeProjection: String
    psi: val foo = getT<Pair<List<Int>, String>>()
    type: String
*/