// IGNORE_BACKEND: JVM_IR
// TODO KT-36829 Optimize 'in' expressions in JVM_IR
fun test1(a: String) = a in "alpha" .. "omega"
fun test2(a: String) = a !in "alpha" .. "omega"

// 0 rangeTo
// 0 contains
// 4 compareTo