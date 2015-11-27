val foo: Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>> = null!!
/*
psi: Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>
type: Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>
    typeParameter: <out A> defined in kotlin.Pair
    typeProjection: List<Int?>
    psi: List<Int?>
    type: List<Int?>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int?
        psi: Int?
        type: Int?

    typeParameter: <out B> defined in kotlin.Pair
    typeProjection: Pair<List<Int?>?, List<Int>?>
    psi: Pair<List<Int?>?, List<Int>?>
    type: Pair<List<Int?>?, List<Int>?>
        typeParameter: <out A> defined in kotlin.Pair
        typeProjection: List<Int?>?
        psi: List<Int?>?
        type: List<Int?>?
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: Int?
            psi: Int?
            type: Int?

        typeParameter: <out B> defined in kotlin.Pair
        typeProjection: List<Int>?
        psi: List<Int>?
        type: List<Int>?
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: Int
            psi: Int
            type: Int
*/