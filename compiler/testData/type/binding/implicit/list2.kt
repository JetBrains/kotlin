fun <T> getT(): T = null!!

val foo = getT<List<String, List<Int>>>()
/*
psi: val foo = getT<List<String, List<Int>>>()
type: [ERROR : List<String, List<Int>>]
*/