fun <T> getT(): T = null!!

val foo = getT<() -> List<Int>>()
/*
psi: val foo = getT<() -> List<Int>>()
type: () -> List<Int>
    typeParameter: <out R> defined in kotlin.Function0
    typeProjection: List<Int>
    psi: val foo = getT<() -> List<Int>>()
    type: List<Int>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int
        psi: val foo = getT<() -> List<Int>>()
        type: Int
*/