fun <T> getT(): T = null!!

val foo = getT<List<in List<Int>>>()
/*
psi: val foo = getT<List<in List<Int>>>()
type: [Error type: Resolution error type (Inconsistent type: List<in List<Int>> (0 parameter has declared variance: out, but argument variance is in))]
*/