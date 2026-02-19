// IGNORE_BACKEND_K1: ANY
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters
// FILE: Sam.java
import org.C;

public interface Sam  {
    String accept(C a);
}

// FILE: test.kt
package org
import Sam

class C(var a: String) {
    fun foo(): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

val foo: context(C) () -> String
    get() = { implicit<C>().foo() }

val samObject = Sam (::foo.get())

fun box(): String {
    with(C("not OK")){
        return samObject.accept(C("OK"))
    }
}