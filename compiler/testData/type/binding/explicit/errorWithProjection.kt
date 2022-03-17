val foo: List<in Int, out Int> = null!!
/*
psi: List<in Int, out Int>
type: [Error type: Type for error type constructor (List)]<in Int, out Int>
    typeParameter: null
    typeProjection: Int
    psi: Int
    type: Int

    typeParameter: null
    typeProjection: Int
    psi: Int
    type: Int
*/