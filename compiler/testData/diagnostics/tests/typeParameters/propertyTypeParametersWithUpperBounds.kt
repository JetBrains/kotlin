
val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T : K<!>, K> K.a: Int get() = 4

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>, K> K.b: Int where T : K
    get() = 4

val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>, K> K.c: Int where T : List<K>
    get() = 4

val <T, K> K.d: Int where K : T
    get() = 4

val <T, K> K.e: Int where K : List<T>
    get() = 4

interface G
val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>T<!>> G.x1: Int where T : G
    get() = 4


val <<!TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER!>X<!>, Y, Z> Z.x2: Int where X : Y, Z : Y
    get() = 4

val <X, Y: Map<Z, X>, Z: List<List<Y>>> Z.x3: Int
    get() = 5

val <X, Y: Map<X, List<Z>>, Z> Map<X, List<Y>>.x4: Int
    get() = 5