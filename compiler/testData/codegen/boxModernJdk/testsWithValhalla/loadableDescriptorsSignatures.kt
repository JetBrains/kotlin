// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

value class Val(val a: Int, val b: Int)
value class Other(val c: Int, val d: Int)

// `Val` only occurs in method signatures here: as a parameter, a return type and a constructor parameter.
class MethodHolder(v: Val) {
    fun take(v: Val) {}
    fun give(): Val = Val(1, 2)
}

interface InterfaceHolder {
    fun take(v: Val)
}

// The erasure of `T` is `Val`, both in the field descriptor and in the method descriptors.
class GenericHolder<T : Val>(val t: T)

fun <T : Val> genericTake(t: T): T = t

// A field type and a type that only occurs in a method signature.
class Mixed(val v: Val) {
    fun take(o: Other) {}
}

interface Source<T> {
    fun get(): T
}

// The override returns `Integer`: its signature boxes the `Int` because the overridden function returns a type parameter.
class IntSource : Source<Int> {
    override fun get(): Int = 42
}

fun box(): String {
    val holder = MethodHolder(Val(1, 2))
    holder.take(Val(3, 4))
    if (holder.give() != Val(1, 2)) return "MethodHolder.give: ${holder.give()}"
    object : InterfaceHolder {
        override fun take(v: Val) {}
    }.take(Val(5, 6))
    if (GenericHolder(Val(7, 8)).t != Val(7, 8)) return "GenericHolder.t"
    if (genericTake(Val(9, 9)) != Val(9, 9)) return "genericTake"
    val mixed = Mixed(Val(1, 1))
    mixed.take(Other(2, 2))
    if (mixed.v != Val(1, 1)) return "Mixed.v"
    val intSource: Source<Int> = IntSource()
    if (intSource.get() != 42) return "IntSource.get: ${intSource.get()}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
