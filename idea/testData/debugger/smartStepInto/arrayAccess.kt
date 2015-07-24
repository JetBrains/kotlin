fun foo() {
    val a = A()
   <caret>a[1]
}

class A {
    fun get(i: Int) = 1
}

// EXISTS: get(Int)