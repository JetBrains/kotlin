// KT-15473 Invalid KFunction byte code signature for callable references

// FILE: GenericSignature.java
package test;

import kotlin.reflect.KFunction;

class Bar extends Foo {
    @Override
    public KFunction<Request> request() {
        return null;
    }
}

// FILE: GenericSignature.kt
package test

class Request(val id: Long)

open class Foo {
    open fun request() = ::Request
}
