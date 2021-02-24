fun <T> getT(): T = null!!

val foo = getT<List<in List<Int>>>()
/*
psi: val foo = getT<List<in List<Int>>>()
type: [ERROR : Inconsistent type: List<in List<Int>> (0 parameter has declared variance: out, but argument variance is in)]
*/