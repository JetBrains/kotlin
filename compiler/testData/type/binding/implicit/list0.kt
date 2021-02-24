fun <T> getT(): T = null!!

val foo = getT<List>()
/*
psi: val foo = getT<List>()
type: [ERROR : Type for getT<List>()]
*/