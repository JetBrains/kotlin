deprecated("Class")
open class Obsolete {
    fun use() {}
}

deprecated("Class")
open class Obsolete2 [deprecated("Constructor")]() {
    fun use() {}
}

trait Generic<T>

open class Derived() : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()

class Derived2() : Derived()

class TypeParam : Generic<<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>>

object Object : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()

class Properties {
    val x : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!> = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
    var y : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!> = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()

    var n : <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>
        get() = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()
        set(value) {}
}

fun param(param: <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>) { param.use() }

fun funcParamReceiver(param: <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>.()->Unit) { <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>().param() }
fun funcParamParam(param: (<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>)->Unit) { param(<!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()) }
fun funcParamRetVal(param: ()-><!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>) { param() }

fun constraint<T: <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>>() {}

fun <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>.receiver() {}

fun retVal(): <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!> = <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>()

fun nullableRetVal(): <!DEPRECATED_SYMBOL_WITH_MESSAGE!>Obsolete<!>? = null