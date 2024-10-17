// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-63855

class Bar<T : Any>(val t: T)
class Foo<T : Any?, out B : Bar<out T & Any>?>(val t: T, val b: B)

typealias Alias<T> = Foo<T, Bar<out T & Any>?>

fun nothing(): Nothing = throw NotImplementedError()
val x: Alias<Int?> get() = nothing()
val y: Foo<Int?, Bar<Int>> get() = nothing()

fun box(): String = "OK"