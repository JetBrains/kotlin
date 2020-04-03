@Deprecated("Class")
open class Obsolete {
    fun use() {}
}

@Deprecated("Class")
open class Obsolete2 @Deprecated("Constructor") constructor() {
    fun use() {}
}

interface Generic<T>

open class Derived() : Obsolete()

class Derived2() : Derived()

class TypeParam : Generic<Obsolete>

object Object : Obsolete()

class Properties {
    val x : Obsolete = Obsolete()
    var y : Obsolete = Obsolete()

    var n : Obsolete
        get() = Obsolete()
        set(value) {}
}

fun param(param: Obsolete) { param.use() }

fun funcParamReceiver(param: Obsolete.()->Unit) { Obsolete().param() }
fun funcParamParam(param: (Obsolete)->Unit) { param(Obsolete()) }
fun funcParamRetVal(param: ()->Obsolete) { param() }

fun <T: Obsolete> constraint() {}

fun Obsolete.receiver() {}

fun retVal(): Obsolete = Obsolete()

fun nullableRetVal(): Obsolete? = null
