val foo: List<(Pair<Int, Float>) -> List<Int>> = null!!
/*
psi: List<(Pair<Int, Float>) -> List<Int>>
type: List<(Pair<Int, Float>) -> List<Int>>
    typeParameter: <out E> defined in kotlin.collections.List
    typeProjection: (Pair<Int, Float>) -> List<Int>
    psi: (Pair<Int, Float>) -> List<Int>
    type: (Pair<Int, Float>) -> List<Int>
        typeParameter: <in P1> defined in kotlin.Function1
        typeProjection: Pair<Int, Float>
        psi: Pair<Int, Float>
        type: Pair<Int, Float>
            typeParameter: <out A> defined in kotlin.Pair
            typeProjection: Int
            psi: Int
            type: Int

            typeParameter: <out B> defined in kotlin.Pair
            typeProjection: Float
            psi: Float
            type: Float

        typeParameter: <out R> defined in kotlin.Function1
        typeProjection: List<Int>
        psi: List<Int>
        type: List<Int>
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: Int
            psi: Int
            type: Int
*/