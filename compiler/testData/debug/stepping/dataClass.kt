// IGNORE_BACKEND: WASM
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

// EXPECTATIONS JVM JVM_IR
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

// EXPECTATIONS JS_IR
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
