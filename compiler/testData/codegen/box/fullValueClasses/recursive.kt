// LANGUAGE: +FullValueClasses, -ForbidValueClassRecursionViaTypeParameters
// CHECK_BYTECODE_LISTING

// Take a look at testData/codegen/box/fullValueClasses/forbiddenRecursive.kt for forbidden recursive samples which though work on JVM.

value class B(val y: Int, val z: B?)
value class E<T>(val a: T)
value class F<T>(val a: T?)

inline fun <T> E<T>.wrap() = E(this)
inline fun <T> F<T>.wrap() = F(this)

value class Bounded2<T: Bounded2<T>>(val x: T?)
value class Bounded4<T: Bounded4<T>>(val x: T?, val y: T?)
value class Bounded5<T: Bounded5<T>?>(val x: T)
value class Bounded6<T: Bounded6<T>?>(val x: T?)
value class Bounded7<T: Bounded7<T>?>(val x: T, val y: T)
value class Bounded8<T: Bounded8<T>?>(val x: T?, val y: T?)

fun box(): String {
    val recursive1 = B(1, B(2, B(3, null)))
    require(recursive1.toString() == "B(y=1, z=B(y=2, z=B(y=3, z=null)))") { recursive1.toString() }
    
    val recursive2 = E(45).wrap().wrap().wrap()
    require(recursive2.toString() == "E(a=E(a=E(a=E(a=45))))") { recursive2.toString() }
    
    val recursive3 = F(null).wrap().wrap().wrap()
    require(recursive3.toString() == "F(a=F(a=F(a=F(a=null))))") { recursive3.toString() }
    
    val bounded2 = Bounded2(null)
    require(bounded2.toString() == "Bounded2(x=null)") { bounded2.toString() }
    
    val bounded4 = Bounded4(null, null)
    require(bounded4.toString() == "Bounded4(x=null, y=null)") { bounded4.toString() }

    val bounded5 = Bounded5(null)
    require(bounded5.toString() == "Bounded5(x=null)") { bounded5.toString() }

    val bounded6 = Bounded6(null)
    require(bounded6.toString() == "Bounded6(x=null)") { bounded6.toString() }

    val bounded7 = Bounded7(null, null)
    require(bounded7.toString() == "Bounded7(x=null, y=null)") { bounded7.toString() }

    val bounded8 = Bounded8(null, null)
    require(bounded8.toString() == "Bounded8(x=null, y=null)") { bounded8.toString() }
    
    return "OK"
}
