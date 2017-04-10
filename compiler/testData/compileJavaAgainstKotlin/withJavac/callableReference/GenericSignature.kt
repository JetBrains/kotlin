// KT-15473 Invalid KFunction byte code signature for callable references

package test

class Request(val id: Long)

open class Foo {
    open fun request() = ::Request
}
