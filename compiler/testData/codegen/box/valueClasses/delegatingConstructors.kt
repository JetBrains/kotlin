// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

data class A(val w: Float)

@JvmInline
value class B(val a: A, val b: A) {

    constructor(aW: Float, aX: Float, aY: Float, aZ: Float,
                bW: Float, bX: Float, bY: Float, bZ: Float) : this(A(aW), A(bW))

    constructor() : this(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f)
}

data class C(val a: B, val b: B) {

    constructor(aW: B, aX: B, aY: B, aZ: B,
                bW: B, bX: B, bY: B, bZ: B) : this(aW, bW)

    constructor() : this(B(), B())
}

fun box(): String {
    require(B().a == A(0.0f))
    require(B().b == A(4.0f))

    require(C().a.a == A(0.0f))
    require(C().a.b == A(4.0f))
    require(C().b.a == A(0.0f))
    require(C().b.b == A(4.0f))
    
    return "OK"
}
