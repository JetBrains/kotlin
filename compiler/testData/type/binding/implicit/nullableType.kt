fun <T> getT(): T = null!!

val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
/*
psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
type: Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>
    typeParameter: <out A> defined in kotlin.Pair
    typeProjection: List<Int?>
    psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
    type: List<Int?>
        typeParameter: <out E> defined in kotlin.collections.List
        typeProjection: Int?
        psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
        type: Int?

    typeParameter: <out B> defined in kotlin.Pair
    typeProjection: Pair<List<Int?>?, List<Int>?>
    psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
    type: Pair<List<Int?>?, List<Int>?>
        typeParameter: <out A> defined in kotlin.Pair
        typeProjection: List<Int?>?
        psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
        type: List<Int?>?
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: Int?
            psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
            type: Int?

        typeParameter: <out B> defined in kotlin.Pair
        typeProjection: List<Int>?
        psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
        type: List<Int>?
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: Int
            psi: val foo = getT<Pair<List<Int?>, Pair<List<Int?>?, List<Int>?>>>()
            type: Int
*/