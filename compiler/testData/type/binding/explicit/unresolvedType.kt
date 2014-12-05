val foo: List<adad<List<dd>>> = null!!
/*
psi: List<adad<List<dd>>>
type: List<[ERROR : adad<List<dd>>]>
    typeParameter: <out E> defined in kotlin.List
    typeProjection: [ERROR : adad<List<dd>>]
    psi: adad<List<dd>>
    type: [ERROR : adad<List<dd>>]
        typeParameter: null
        typeProjection: List<[ERROR : dd]>
        psi: List<dd>
        type: List<[ERROR : dd]>
            typeParameter: <out E> defined in kotlin.List
            typeProjection: [ERROR : dd]
            psi: dd
            type: [ERROR : dd]
*/