

val foo: Pair<List<Int>, String> = null!!
/*
psi: Pair<List<Int>, String>
type: Pair<List<Int>, String>
    typeParameter: <out A> defined in kotlin.Pair
    typeProjection: List<Int>
    psi: List<Int>
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: Int
        type: Int

    typeParameter: <out B> defined in kotlin.Pair
    typeProjection: String
    psi: String
    type: String
*/