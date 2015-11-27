val foo: List<in List<Int>> = null!!
/*
psi: List<in List<Int>>
type: List<in List<Int>>
    typeParameter: <out E> defined in kotlin.collections.List
    typeProjection: in List<Int>
    psi: List<Int>
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: Int
        type: Int
*/