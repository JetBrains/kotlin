// NAME: B
open class A
typealias MyA = A
// SIBLING:
fun fA(p:<selection>A</selection>): MyA = MyA()
fun fB(p:A): A = A()