// !DIAGNOSTICS: -UNUSED_PARAMETER

class R<T: R<T>>

open class Base<T> {
    fun foo(r: R<*>) {}
}

class Derived: Base<String>()