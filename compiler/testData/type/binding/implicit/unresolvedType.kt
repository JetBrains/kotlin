fun <T> getT(): T = null!!

val foo = getT<List<adad<List<dd>>>()
/*
psi: val foo = getT<List<adad<List<dd>>>()
type: [ERROR : Type for getT<List<adad<List<dd>>>()]
*/