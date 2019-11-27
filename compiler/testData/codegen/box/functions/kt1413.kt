// IGNORE_BACKEND_FIR: JVM_IR
package t

interface I{
    fun f()
}

class Test{
    fun foo(){
        val i : I = object : I {
            override fun f() {
                fun local(){
                    bar()
                }
                 local()
            }
        }
        i.f()
    }

    fun bar(){}
}

fun box() : String {
    Test().foo()
    return "OK"
}
