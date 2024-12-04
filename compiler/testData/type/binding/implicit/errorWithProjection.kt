fun <T> getT(): T = null!!

val foo = getT<List<in Int, out Int>>()
/*
psi: val foo = getT<List<in Int, out Int>>()
type: [Error type: Not found recorded type for getT<List<in Int, out Int>>()]
*/