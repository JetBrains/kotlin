// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

value class A(val y: Int, val z: A)
value class B(val y: Int, val z: B?)

value class C(val a: C?, val b: C, val c: C, val d: C?, val e: D)

value class D(val a: C)
value class E<T>(val a: T)
value class F<T>(val a: T?)

fun C.wrap() = C(null, this, this, this, D(this))
inline fun <T> E<T>.wrap() = E(this)
inline fun <T> F<T>.wrap() = F(this)

fun box(): String {
    val recursive1 = B(1, B(2, B(3, null)))
    require(recursive1.toString() == "B(y=1, z=B(y=2, z=B(y=3, z=null)))") { recursive1.toString() }
    
    val recursive2 = E(45).wrap().wrap().wrap()
    require(recursive2.toString() == "E(a=E(a=E(a=E(a=45))))") { recursive2.toString() }
    
    val recursive3 = F(null).wrap().wrap().wrap()
    require(recursive3.toString() == "F(a=F(a=F(a=F(a=null))))") { recursive3.toString() }
    
    return "OK"
}
