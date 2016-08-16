class A

operator fun A.plusAssign(s: String) {}

fun test() { // <<< augmentedAssignment2.txt
    val a = A()
    a += ""
}
