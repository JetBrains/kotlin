fun <T> getT(): T = null!!

val foo = getT<List<String, List<Int>>>()
/*
psi: val foo = getT<List<String, List<Int>>>()
type: [ERROR : Type for getT<List<String, List<Int>>>()]
*/