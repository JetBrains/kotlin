package test

import dependency.*

class A

class B {
    var a b<caret>y A()
}

// MULTIRESOLVE
// REF: (for T in dependency).get(R,jet.PropertyMetadata)
// REF: (for T in dependency).set(R,jet.PropertyMetadata,jet.Int)
