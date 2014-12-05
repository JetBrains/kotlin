val foo: List<in Int> = null!!
/*
psi: List<in Int>
type: List<in Int>
    typeParameter: <out E> defined in kotlin.List
    typeProjection: in Int
    psi: Int
    type: Int
*/