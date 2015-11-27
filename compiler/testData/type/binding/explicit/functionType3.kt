val foo: () -> List<Int> = null!!
/*
psi: () -> List<Int>
type: () -> List<Int>
    typeParameter: <out R> defined in kotlin.Function0
    typeProjection: List<Int>
    psi: List<Int>
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: Int
        type: Int
*/