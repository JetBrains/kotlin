// IGNORE_BACKEND_FIR: JVM_IR
// https://youtrack.jetbrains.com/issue/KT-52236/Different-modality-in-psi-and-fir
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@JvmInline
value class Public(val x: Int, val y: Int) {
    companion object {
        // TODO
    }
}

@JvmInline
value class Internal(internal val x: Int, internal val y: Int) {
    companion object {
        // @JvmStatic
        // TODO
    }
}

@JvmInline
value class Private(private val x: Int, private val y: Int)

@JvmInline
value class PublicPublic(val value: Public)

@JvmInline
value class InternalPublic(internal val value: Public)

@JvmInline
value class PrivatePublic(private val value: Public)

@JvmInline
value class PublicInternal(val value: Internal)

@JvmInline
value class InternalInternal(internal val value: Internal)

@JvmInline
value class PrivateInternal(private val value: Internal)

@JvmInline
value class PublicPrivate(val value: Private)

@JvmInline
value class InternalPrivate(internal val value: Private)

@JvmInline
value class PrivatePrivate(private val value: Private)

class Regular {
    var x1: Public = Public(1, 2)
    internal var x2: Public = Public(3, 4)
    private var x3: Public = Public(5, 6)

    var x4: Public = Public(7, 8)
        private set
    internal var x5: Public = Public(9, 10)
        private set
    
    var x6: Public = Public(11, 12)
        internal set
    
    
    var y1: Internal = Internal(13, 14)
    internal var y2: Internal = Internal(15, 16)
    private var y3: Internal = Internal(17, 18)

    var y4: Internal = Internal(19, 20)
        private set
    internal var y5: Internal = Internal(21, 22)
        private set
    
    var y6: Internal = Internal(23, 24)
        internal set
    
    
    var z1: Private = Private(25, 26)
    internal var z2: Private = Private(27, 28)
    private var z3: Private = Private(29, 30)

    var z4: Private = Private(31, 32)
        private set
    internal var z5: Private = Private(33, 34)
        private set
    
    var z6: Private = Private(35, 36)
        internal set
    
    companion object {
        // TODO @JvmStatic
        var staticX1: Public = Public(-1, -2)
        internal var staticX2: Public = Public(-3, -4)
        private var staticX3: Public = Public(-5, -6)

        var staticX4: Public = Public(-7, -8)
            private set
        internal var staticX5: Public = Public(-9, -10)
            private set

        var staticX6: Public = Public(-11, -12)
            internal set


        var staticY1: Internal = Internal(-13, -14)
        internal var staticY2: Internal = Internal(-15, -16)
        private var staticY3: Internal = Internal(-17, -18)

        var staticY4: Internal = Internal(-19, -20)
            private set
        internal var staticY5: Internal = Internal(-21, -22)
            private set

        var staticY6: Internal = Internal(-23, -24)
            internal set


        var staticZ1: Private = Private(-25, -26)
        internal var staticZ2: Private = Private(-27, -28)
        private var staticZ3: Private = Private(-29, -30)

        var staticZ4: Private = Private(-31, -32)
            private set
        internal var staticZ5: Private = Private(-33, -34)
            private set

        var staticZ6: Private = Private(-35, -36)
            internal set
        
    }
    
    // TODO statics
    fun callAll() {
        z6
        x3
        x3.x
        x3.y
    }
}

fun box(): String {
    Regular().callAll()
    return "OK"
}