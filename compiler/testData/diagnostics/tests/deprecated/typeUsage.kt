@Deprecated("Class")
open class Obsolete {
    fun use() {}
}

@Deprecated("Class")
open class Obsolete2 @Deprecated("Constructor") constructor() {
    fun use() {}
}

interface Generic<T>

open class Derived() : <!DEPRECATION!>Obsolete<!>()

class Derived2() : Derived()

class TypeParam : Generic<<!DEPRECATION!>Obsolete<!>>

object Object : <!DEPRECATION!>Obsolete<!>()

class Properties {
    val x : <!DEPRECATION!>Obsolete<!> = <!DEPRECATION!>Obsolete<!>()
    var y : <!DEPRECATION!>Obsolete<!> = <!DEPRECATION!>Obsolete<!>()

    var n : <!DEPRECATION!>Obsolete<!>
        get() = <!DEPRECATION!>Obsolete<!>()
        set(value) {}
}

fun param(param: <!DEPRECATION!>Obsolete<!>) { param.use() }

fun funcParamReceiver(param: <!DEPRECATION!>Obsolete<!>.()->Unit) { <!DEPRECATION!>Obsolete<!>().param() }
fun funcParamParam(param: (<!DEPRECATION!>Obsolete<!>)->Unit) { param(<!DEPRECATION!>Obsolete<!>()) }
fun funcParamRetVal(param: ()-><!DEPRECATION!>Obsolete<!>) { param() }

fun <T: <!DEPRECATION!>Obsolete<!>> constraint() {}

fun <!DEPRECATION!>Obsolete<!>.receiver() {}

fun retVal(): <!DEPRECATION!>Obsolete<!> = <!DEPRECATION!>Obsolete<!>()

fun nullableRetVal(): <!DEPRECATION!>Obsolete<!>? = null
