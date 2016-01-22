fun <T> getT(): T = null!!

val foo = getT<List<String, List<Int>>>()
/*
psi: val foo = getT<List<String, List<Int>>>()
type: [ERROR : List]<String, List<Int>>
    typeParameter: null
    typeProjection: String
    psi: val foo = getT<List<String, List<Int>>>()
    type: String

    typeParameter: null
    typeProjection: List<Int>
    psi: val foo = getT<List<String, List<Int>>>()
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: val foo = getT<List<String, List<Int>>>()
        type: Int
*/