val foo: List<String, List<Int>> = null!!
/*
psi: List<String, List<Int>>
type: [ERROR : List]<String, List<Int>>
    typeParameter: null
    typeProjection: String
    psi: String
    type: String

    typeParameter: null
    typeProjection: List<Int>
    psi: List<Int>
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: Int
        type: Int
*/