// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JVM_IR

interface I<T: A> {
    fun f(x: T) = println(x)
    fun g(x: T = error("OK")) = println(x)
    fun T.f(x: T) = println(x)
    fun T.g(x: T = error("OK")) = println(x)
}

@JvmInline
value class A(val x: Int, val y: Int): I<Nothing>

@JvmInline
value class B(val x: Int, val y: Int): I<Nothing> {
    override fun f(x: Nothing) = println(this)
    override fun g(x: Nothing) = println(this)
    fun h(x: Nothing) = println(this)
    fun t(x: Nothing = error("OK")) = println(this)
    override fun Nothing.f(x: Nothing) = println(this@B)
    override fun Nothing.g(x: Nothing) = println(this@B)
    fun Nothing.h(x: Nothing) = println(this@B)
    fun Nothing.t(x: Nothing = error("OK")) = println(this@B)
}

fun box(): String {
    require(runCatching { A(1, 2).f(error("OK1")) }.exceptionOrNull()!!.message!! == "OK1")
    require(runCatching { B(1, 2).f(error("OK2")) }.exceptionOrNull()!!.message!! == "OK2")
    require(runCatching { A(1, 2).g(error("OK3")) }.exceptionOrNull()!!.message!! == "OK3")
    require(runCatching { B(1, 2).g(error("OK4")) }.exceptionOrNull()!!.message!! == "OK4")
    require(runCatching { A(1, 2).g() }.exceptionOrNull()!!.message!! == "OK")
    require(runCatching { B(1, 2).g() }.exceptionOrNull()!!.message!! == "OK")
    require(runCatching { B(1, 2).h(error("OK5")) }.exceptionOrNull()!!.message!! == "OK5")
    require(runCatching { B(1, 2).t(error("OK6")) }.exceptionOrNull()!!.message!! == "OK6")
    require(runCatching { B(1, 2).t() }.exceptionOrNull()!!.message!! == "OK")

    require(runCatching { A(1, 2).run { error("OK1").f(error("OK")) } }.exceptionOrNull()!!.message!! == "OK1")
    require(runCatching { B(1, 2).run { error("OK2").f(error("OK")) } }.exceptionOrNull()!!.message!! == "OK2")
    require(runCatching { A(1, 2).run { error("OK3").g(error("OK")) } }.exceptionOrNull()!!.message!! == "OK3")
    require(runCatching { B(1, 2).run { error("OK4").g(error("OK")) } }.exceptionOrNull()!!.message!! == "OK4") 
    require(runCatching { A(1, 2).run { error("OK5").g() } }.exceptionOrNull()!!.message!! == "OK5")
    require(runCatching { B(1, 2).run { error("OK6").g() } }.exceptionOrNull()!!.message!! == "OK6")
    require(runCatching { B(1, 2).run { error("OK7").h(error("OK")) } }.exceptionOrNull()!!.message!! == "OK7")
    require(runCatching { B(1, 2).run { error("OK8").t(error("OK")) } }.exceptionOrNull()!!.message!! == "OK8")
    require(runCatching { B(1, 2).run { error("OK9").t() } }.exceptionOrNull()!!.message!! == "OK9")
    
    return "OK"
}
