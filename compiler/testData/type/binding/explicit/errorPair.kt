val foo: Pair<Pair<List<Int>>, String> = null!!
/*
psi: Pair<Pair<List<Int>>, String>
type: Pair<[Error type: Type for error type constructor (Pair)]<List<Int>>, String>
    typeParameter: <out A> defined in kotlin.Pair
    typeProjection: [Error type: Type for error type constructor (Pair)]<List<Int>>
    psi: Pair<List<Int>>
    type: [Error type: Type for error type constructor (Pair)]<List<Int>>
        typeParameter: null
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