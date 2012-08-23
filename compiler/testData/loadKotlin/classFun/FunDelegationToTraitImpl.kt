package test

// test composed from KT-2193

trait A {
    open fun f(): String = "test"
}

class B() : A