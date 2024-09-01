package test

interface A {
    interface NestedA
}

interface B : A

interface C : A.NestedA

interface D

interface E : D

interface F : E

interface G

abstract class X1 : B, D

open class X2 : B, C

abstract class X3 : D, F

class Y : X1(), G

class Z : X2(), G, F

object O : X3(), C

val v<caret_type1>1: O = O

val v<caret_type2>2: D = Y()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/D
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
