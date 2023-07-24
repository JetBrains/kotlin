import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: F.A, p: KProperty<*>): Int = 1
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
