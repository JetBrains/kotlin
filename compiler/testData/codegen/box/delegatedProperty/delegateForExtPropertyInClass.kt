class Delegate {
    fun get(t: F.A, p: PropertyMetadata): Int = 1
}

class F {
    val A.prop: Int by Delegate()

    class A {
    }
    
    fun foo(): Int {
       return A().prop 
    }
}

fun box(): String {
    return if(F().foo() == 1) "OK" else "fail"
}