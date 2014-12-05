fun <T> getT(): T = null!!

val fml = getT<Int>()
/*
psi: val fml = getT<Int>()
type: Int
*/