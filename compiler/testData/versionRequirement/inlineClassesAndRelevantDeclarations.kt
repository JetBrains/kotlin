package test

@Suppress("UNSUPPORTED_FEATURE")
inline class IC(val x: String)

typealias ICAlias = IC

class Ctor(ic: IC)

fun simpleFun(f: IC) {}
fun aliasedFun(f: ICAlias) {}

val simpleProp: IC = IC("")

fun result(r: List<Result<Any>?>) {}

abstract class Foo : List<IC>
interface Bar<T : IC>
