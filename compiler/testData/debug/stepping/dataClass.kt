// IGNORE_BACKEND_K2: WASM
// FILE: test.kt

data class D(val i: Int, val s: String)

data class E(val i: Int, val s: String) {
    override fun toString() = "OK"
    override fun equals(other: Any?) = false
    override fun hashCode() = 42
    fun copy() = E(i, s)
}

fun box() {
    val d = D(1, "a")
    d.equals(D(1, "a"))
    d.hashCode()
    d.toString()
    val (i, s) = d
    d.copy()
    val e = E(1, "a")
    e.equals(E(1, "a"))
    e.hashCode()
    e.toString()
    val (s2, i2) = e
    e.copy()
}

// EXPECTATIONS JVM_IR
// test.kt:14 box
// test.kt:4 <init>
// test.kt:14 box
// test.kt:15 box
// test.kt:4 <init>
// test.kt:15 box
// test.kt:16 box
// test.kt:17 box
// test.kt:18 box
// test.kt:19 box
// test.kt:4 <init>
// test.kt:-1 copy
// test.kt:19 box
// test.kt:20 box
// test.kt:6 <init>
// test.kt:20 box
// test.kt:21 box
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:21 box
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:22 box
// test.kt:23 box
// test.kt:7 toString
// test.kt:23 box
// test.kt:24 box
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:10 copy
// test.kt:25 box
// test.kt:26 box

// EXPECTATIONS ClassicFrontend JS_IR
// test.kt:14 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:1 equals
// test.kt:16 box
// test.kt:1 hashCode
// test.kt:1 hashCode
// test.kt:17 box
// test.kt:1 toString
// test.kt:18 box
// test.kt:1 component1
// test.kt:18 box
// test.kt:1 component2
// test.kt:19 box
// test.kt:1 copy$default
// test.kt:1 copy$default
// test.kt:1 copy
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:20 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:23 box
// test.kt:7 toString
// test.kt:24 box
// test.kt:1 component1
// test.kt:24 box
// test.kt:1 component2
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:26 box

// EXPECTATIONS FIR JS_IR // TODO: There is an inconsistency in names between K1 and K2. This should be fixed in KT-64435.
// test.kt:14 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:1 protoOf.equals
// test.kt:16 box
// test.kt:1 protoOf.hashCode
// test.kt:1 protoOf.hashCode
// test.kt:17 box
// test.kt:1 protoOf.toString
// test.kt:18 box
// test.kt:1 protoOf.component1_7eebsc_k$
// test.kt:18 box
// test.kt:1 protoOf.component2_7eebsb_k$
// test.kt:19 box
// test.kt:1 protoOf.copy$default_8mg6yi_k$
// test.kt:1 protoOf.copy$default_8mg6yi_k$
// test.kt:1 protoOf.copy_xhhsuv_k$
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:4 <init>
// test.kt:20 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:21 box
// test.kt:8 equals
// test.kt:22 box
// test.kt:9 hashCode
// test.kt:23 box
// test.kt:7 toString
// test.kt:24 box
// test.kt:1 protoOf.component1_7eebsc_k$
// test.kt:24 box
// test.kt:1 protoOf.component2_7eebsb_k$
// test.kt:25 box
// test.kt:10 copy
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:6 <init>
// test.kt:26 box

// EXPECTATIONS WASM
// test.kt:14 $box (12, 14, 17, 17, 17, 12)
// test.kt:4 $D.<init> (13, 25, 25, 25, 39, 13, 25, 25, 25, 39, 13, 25, 25, 25, 39)
// test.kt:15 $box (4, 13, 15, 18, 18, 18, 18, 13, 6, 6)
// test.kt:16 $box (4, 6, 6)
// test.kt:17 $box (4, 6)
// test.kt:18 $box (17, 9, 9, 17, 12, 12)
// test.kt:19 $box (4, 6, 6, 6, 6)
// test.kt:20 $box (12, 14, 17, 17, 17, 12)
// test.kt:6 $E.<init> (13, 25, 25, 25, 39, 13, 25, 25, 25, 39, 13, 25, 25, 25, 39)
// test.kt:21 $box (4, 13, 15, 18, 18, 18, 18, 13, 6, 6)
// test.kt:8 $E.equals (39, 44)
// test.kt:22 $box (4, 6, 6)
// test.kt:9 $E.hashCode (30, 32)
// test.kt:23 $box (4, 6)
// test.kt:7 $E.toString (30, 30, 30, 30, 34)
// test.kt:24 $box (19, 9, 9, 19, 13, 13)
// test.kt:25 $box (4, 6)
// test.kt:10 $E.copy (17, 19, 19, 22, 22, 17, 24)
// test.kt:26 $box
