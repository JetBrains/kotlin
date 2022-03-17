val foo: List<adad<List<dd>>> = null!!
/*
psi: List<adad<List<dd>>>
type: List<[Error type: Unresolved type for adad<List<dd>>]<List<[Error type: Unresolved type for dd]>>>
    typeParameter: <out E> defined in kotlin.collections.List
    typeProjection: [Error type: Unresolved type for adad<List<dd>>]<List<[Error type: Unresolved type for dd]>>
    psi: adad<List<dd>>
    type: [Error type: Unresolved type for adad<List<dd>>]<List<[Error type: Unresolved type for dd]>>
        typeParameter: null
        typeProjection: List<[Error type: Unresolved type for dd]>
        psi: List<dd>
        type: List<[Error type: Unresolved type for dd]>
            typeParameter: <out E> defined in kotlin.collections.List
            typeProjection: [Error type: Unresolved type for dd]
            psi: dd
            type: [Error type: Unresolved type for dd]
*/