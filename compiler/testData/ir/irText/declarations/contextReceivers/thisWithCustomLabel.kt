// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_FIR: JVM_IR

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(labelAInt@A<Int>, A<String>, labelB@B) fun f() {
    this@labelAInt.a.toFloat()
    this@A.a.length
    this@labelB.b
}

context(labelAInt@A<Int>, A<String>, labelB@B) val C.p: Int
    get() {
        this@labelAInt.a.toFloat()
        this@A.a.length
        this@labelB.b
        this@C.c
        this@p.c
        this.c
        return 1
    }