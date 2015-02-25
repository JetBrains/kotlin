fun <T> getT(): T = null!!

val foo = getT<List<in Int, out Int>>()
/*
psi: val foo = getT<List<in Int, out Int>>()
type: [ERROR : List]<in Int, out Int>
    typeParameter: null
    typeProjection: in Int
    psi: val foo = getT<List<in Int, out Int>>()
    type: Int

    typeParameter: null
    typeProjection: out Int
    psi: val foo = getT<List<in Int, out Int>>()
    type: Int
*/