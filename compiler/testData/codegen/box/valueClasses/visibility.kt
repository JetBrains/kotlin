// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses
// JVM_ABI_K1_K2_DIFF: KT-63984

// MODULE: dependency
// FILE: dependency.kt

@JvmInline
value class MfvcDependency(val x: Int, val y: Int) {
    val z: Int
        get() = 1
    val t: MfvcDependency
        get() = MfvcDependency(10, 20)
    companion object {
        var x: Int = -100
        val y: Int
            get() = 1
        var z: MfvcDependency = MfvcDependency(10, 20)
        val t: MfvcDependency
            get() = MfvcDependency(10, 20)

        @JvmStatic
        var xStatic: Int = -100
        @JvmStatic
        val yStatic: Int
            get() = 1
        @JvmStatic
        var zStatic: MfvcDependency = MfvcDependency(10, 20)
        @JvmStatic
        val tStatic: MfvcDependency
            get() = MfvcDependency(10, 20)
    }
}
class RegularDependency {
    var x: Int = -100
    val y: Int
        get() = 1
    var z: MfvcDependency = MfvcDependency(10, 20)
    val t: MfvcDependency
        get() = MfvcDependency(10, 20)
    
    companion object {
        var x: Int = -100
        val y: Int
            get() = 1
        var z: MfvcDependency = MfvcDependency(10, 20)
        val t: MfvcDependency
            get() = MfvcDependency(10, 20)
        
        @JvmStatic
        var xStatic: Int = -100
        @JvmStatic
        val yStatic: Int
            get() = 1
        @JvmStatic
        var zStatic: MfvcDependency = MfvcDependency(10, 20)
        @JvmStatic
        val tStatic: MfvcDependency
            get() = MfvcDependency(10, 20)
    }
}

// MODULE: main(dependency)
// FILE: main.kt

@JvmInline
value class Public(val x: Int, val y: Int) {
    companion object {
        var x: Int = -100
        val y: Int
            get() = 1
        var z: Public = Public(10, 20)
        val t: Public
            get() = Public(10, 20)
    }
}

@JvmInline
value class Internal(internal val x: Int, internal val y: Int) {
    companion object {
        @JvmStatic
        var x: Int = -100

        @JvmStatic
        val y: Int
            get() = 1
        var z: Internal = Internal(10, 20)

        @JvmStatic
        val t: Internal
            get() = Internal(10, 20)
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

        @JvmStatic
        var jvmStaticX1: Public = Public(-1, -2)

        @JvmStatic
        internal var jvmStaticX2: Public = Public(-3, -4)

        @JvmStatic
        private var jvmStaticX3: Public = Public(-5, -6)

        @JvmStatic
        var jvmStaticX4: Public = Public(-7, -8)
            private set

        @JvmStatic
        internal var jvmStaticX5: Public = Public(-9, -10)
            private set

        @JvmStatic
        var jvmStaticX6: Public = Public(-11, -12)
            internal set


        @JvmStatic
        var jvmStaticY1: Internal = Internal(-13, -14)

        @JvmStatic
        internal var jvmStaticY2: Internal = Internal(-15, -16)

        @JvmStatic
        private var jvmStaticY3: Internal = Internal(-17, -18)

        @JvmStatic
        var jvmStaticY4: Internal = Internal(-19, -20)
            private set

        @JvmStatic
        internal var jvmStaticY5: Internal = Internal(-21, -22)
            private set

        @JvmStatic
        var jvmStaticY6: Internal = Internal(-23, -24)
            internal set


        @JvmStatic
        var jvmStaticZ1: Private = Private(-25, -26)

        @JvmStatic
        internal var jvmStaticZ2: Private = Private(-27, -28)

        @JvmStatic
        private var jvmStaticZ3: Private = Private(-29, -30)

        @JvmStatic
        var jvmStaticZ4: Private = Private(-31, -32)
            private set

        @JvmStatic
        internal var jvmStaticZ5: Private = Private(-33, -34)
            private set

        @JvmStatic
        var jvmStaticZ6: Private = Private(-35, -36)
            internal set

    }

    fun callAll() {
        x1; x2; x3; x4; x5; x6
        x1.x; x1.y; x2.x; x2.y; x3.x; x3.y; x4.x; x4.y; x5.x; x5.y; x6.x; x6.y
        x1 = x1; x2 = x2; x3 = x3; x4 = x4; x5 = x5; x6 = x6

        y1; y2; y3; y4; y5; y6
        y1.x; y1.y; y2.x; y2.y; y3.x; y3.y; y4.x; y4.y; y5.x; y5.y; y6.x; y6.y
        y1 = y1; y2 = y2; y3 = y3; y4 = y4; y5 = y5; y6 = y6

        z1; z2; z3; z4; z5; z6
        z1 = z1; z2 = z2; z3 = z3; z4 = z4; z5 = z5; z6 = z6


        staticX1; staticX2; staticX3; staticX4; staticX5; staticX6
        staticX1.x; staticX1.y; staticX2.x; staticX2.y; staticX3.x; staticX3.y; staticX4.x; staticX4.y; staticX5.x; staticX5.y; staticX6.x; staticX6.y
        staticX1 = staticX1; staticX2 = staticX2; staticX3 = staticX3; staticX4 = staticX4; staticX5 = staticX5; staticX6 = staticX6

        staticY1; staticY2; staticY3; staticY4; staticY5; staticY6
        staticY1.x; staticY1.y; staticY2.x; staticY2.y; staticY3.x; staticY3.y; staticY4.x; staticY4.y; staticY5.x; staticY5.y; staticY6.x; staticY6.y
        staticY1 = staticY1; staticY2 = staticY2; staticY3 = staticY3; staticY4 = staticY4; staticY5 = staticY5; staticY6 = staticY6

        staticZ1; staticZ2; staticZ3; staticZ4; staticZ5; staticZ6
        staticZ1 = staticZ1; staticZ2 = staticZ2; staticZ3 = staticZ3; staticZ4 = staticZ4; staticZ5 = staticZ5; staticZ6 = staticZ6


        jvmStaticX1; jvmStaticX2; jvmStaticX3; jvmStaticX4; jvmStaticX5; jvmStaticX6
        jvmStaticX1.x; jvmStaticX1.y; jvmStaticX2.x; jvmStaticX2.y; jvmStaticX3.x; jvmStaticX3.y; jvmStaticX4.x; jvmStaticX4.y; jvmStaticX5.x; jvmStaticX5.y; jvmStaticX6.x; jvmStaticX6.y
        jvmStaticX1 = jvmStaticX1; jvmStaticX2 = jvmStaticX2; jvmStaticX3 = jvmStaticX3; jvmStaticX4 = jvmStaticX4; jvmStaticX5 =
            jvmStaticX5; jvmStaticX6 = jvmStaticX6

        jvmStaticY1; jvmStaticY2; jvmStaticY3; jvmStaticY4; jvmStaticY5; jvmStaticY6
        jvmStaticY1.x; jvmStaticY1.y; jvmStaticY2.x; jvmStaticY2.y; jvmStaticY3.x; jvmStaticY3.y; jvmStaticY4.x; jvmStaticY4.y; jvmStaticY5.x; jvmStaticY5.y; jvmStaticY6.x; jvmStaticY6.y
        jvmStaticY1 = jvmStaticY1; jvmStaticY2 = jvmStaticY2; jvmStaticY3 = jvmStaticY3; jvmStaticY4 = jvmStaticY4; jvmStaticY5 =
            jvmStaticY5; jvmStaticY6 = jvmStaticY6

        jvmStaticZ1; jvmStaticZ2; jvmStaticZ3; jvmStaticZ4; jvmStaticZ5; jvmStaticZ6
        jvmStaticZ1 = jvmStaticZ1; jvmStaticZ2 = jvmStaticZ2; jvmStaticZ3 = jvmStaticZ3; jvmStaticZ4 = jvmStaticZ4; jvmStaticZ5 =
            jvmStaticZ5; jvmStaticZ6 = jvmStaticZ6
    }
}

fun box(): String {
    val r = Regular()
    r.apply {
        callAll()


        x1; x2; x4; x5; x6
        x1.x; x1.y; x2.x; x2.y; x4.x; x4.y; x5.x; x5.y; x6.x; x6.y
        x1 = x1; x2 = x2; x6 = x6

        y1; y2; y4; y5; y6
        y1.x; y1.y; y2.x; y2.y; y4.x; y4.y; y5.x; y5.y; y6.x; y6.y
        y1 = y1; y2 = y2; y6 = y6

        z1; z2; z4; z5; z6
        z1 = z1; z2 = z2; z6 = z6
    }

    Regular.Companion.apply {
        staticX1; staticX2; staticX4; staticX5; staticX6
        staticX1.x; staticX1.y; staticX2.x; staticX2.y; staticX4.x; staticX4.y; staticX5.x; staticX5.y; staticX6.x; staticX6.y
        staticX1 = staticX1; staticX2 = staticX2; staticX6 = staticX6

        staticY1; staticY2; staticY4; staticY5; staticY6
        staticY1.x; staticY1.y; staticY2.x; staticY2.y; staticY4.x; staticY4.y; staticY5.x; staticY5.y; staticY6.x; staticY6.y
        staticY1 = staticY1; staticY2 = staticY2; staticY6 = staticY6

        staticZ1; staticZ2; staticZ4; staticZ5; staticZ6
        staticZ1 = staticZ1; staticZ2 = staticZ2; staticZ6 = staticZ6


        jvmStaticX1; jvmStaticX2; jvmStaticX4; jvmStaticX5; jvmStaticX6
        jvmStaticX1.x; jvmStaticX1.y; jvmStaticX2.x; jvmStaticX2.y; jvmStaticX4.x; jvmStaticX4.y; jvmStaticX5.x; jvmStaticX5.y; jvmStaticX6.x; jvmStaticX6.y
        jvmStaticX1 = jvmStaticX1; jvmStaticX2 = jvmStaticX2; jvmStaticX6 = jvmStaticX6

        jvmStaticY1; jvmStaticY2; jvmStaticY4; jvmStaticY5; jvmStaticY6
        jvmStaticY1.x; jvmStaticY1.y; jvmStaticY2.x; jvmStaticY2.y; jvmStaticY4.x; jvmStaticY4.y; jvmStaticY5.x; jvmStaticY5.y; jvmStaticY6.x; jvmStaticY6.y
        jvmStaticY1 = jvmStaticY1; jvmStaticY2 = jvmStaticY2; jvmStaticY6 = jvmStaticY6

        jvmStaticZ1; jvmStaticZ2; jvmStaticZ4; jvmStaticZ5; jvmStaticZ6
        jvmStaticZ1 = jvmStaticZ1; jvmStaticZ2 = jvmStaticZ2; jvmStaticZ6 = jvmStaticZ6
    }
    
    require(MfvcDependency.x == -100)
    require(MfvcDependency.xStatic == -100)
    require(MfvcDependency.y == 1)
    require(MfvcDependency.yStatic == 1)
    require(MfvcDependency.z == MfvcDependency(10, 20))
    require(MfvcDependency.zStatic == MfvcDependency(10, 20))
    require(MfvcDependency(1, 2).x == 1)
    require(MfvcDependency(1, 2).y == 2)
    require(MfvcDependency(1, 2).z == 1)
    require(MfvcDependency(1, 2).t == MfvcDependency(10, 20))
    
    require(RegularDependency.x == -100)
    require(RegularDependency.xStatic == -100)
    require(RegularDependency.y == 1)
    require(RegularDependency.yStatic == 1)
    require(RegularDependency.z == MfvcDependency(10, 20))
    require(RegularDependency.zStatic == MfvcDependency(10, 20))
    require(RegularDependency().x == -100)
    require(RegularDependency().y == 1)
    require(RegularDependency().z == MfvcDependency(10, 20))
    require(RegularDependency().t == MfvcDependency(10, 20))

    return "OK"
}
