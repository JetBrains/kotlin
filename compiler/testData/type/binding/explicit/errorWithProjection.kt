val foo: List<in Int, out Int> = null!!
/*
psi: List<in Int, out Int>
type: [ERROR : List]<in Int, out Int>
    typeParameter: null
    typeProjection: Int
    psi: Int
    type: Int

    typeParameter: null
    typeProjection: Int
    psi: Int
    type: Int
*/