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
// test.kt:13 box
// test.kt:3 <init>
// test.kt:13 box
// test.kt:14 box
// test.kt:3 <init>
// test.kt:14 box
// test.kt:15 box
// test.kt:16 box
// test.kt:17 box
// test.kt:18 box
// test.kt:3 <init>
// test.kt:-1 copy
// test.kt:18 box
// test.kt:19 box
// test.kt:5 <init>
// test.kt:19 box
// test.kt:20 box
// test.kt:5 <init>
// test.kt:20 box
// test.kt:7 equals
// test.kt:20 box
// test.kt:21 box
// test.kt:8 hashCode
// test.kt:21 box
// test.kt:22 box
// test.kt:6 toString
// test.kt:22 box
// test.kt:23 box
// test.kt:24 box
// test.kt:9 copy
// test.kt:5 <init>
// test.kt:9 copy
// test.kt:24 box
// test.kt:25 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:3 D
// test.kt:3 D
// test.kt:14 box
// test.kt:3 D
// test.kt:3 D
// test.kt:14 box
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:1 D.equals
// test.kt:15 box
// test.kt:1 D.hashCode
// test.kt:1 D.hashCode
// test.kt:1 D.hashCode
// test.kt:16 box
// test.kt:1 D.toString
// test.kt:1 D.toString
// test.kt:17 box
// test.kt:17 box
// test.kt:1 D.component1_7eebsc_k$
// test.kt:1 D.component1_7eebsc_k$
// test.kt:17 box
// test.kt:1 D.component2_7eebsb_k$
// test.kt:1 D.component2_7eebsb_k$
// test.kt:18 box
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:1 D.copy_t8q04r_k$
// test.kt:3 D
// test.kt:3 D
// test.kt:1 D.copy_t8q04r_k$
// test.kt:1 D.copy$default_cbhffz_k$
// test.kt:19 box
// test.kt:5 E
// test.kt:5 E
// test.kt:20 box
// test.kt:5 E
// test.kt:5 E
// test.kt:20 box
// test.kt:7 E.equals
// test.kt:7 E.equals
// test.kt:21 box
// test.kt:8 E.hashCode
// test.kt:8 E.hashCode
// test.kt:22 box
// test.kt:6 E.toString
// test.kt:6 E.toString
// test.kt:23 box
// test.kt:23 box
// test.kt:1 E.component1_7eebsc_k$
// test.kt:1 E.component1_7eebsc_k$
// test.kt:23 box
// test.kt:1 E.component2_7eebsb_k$
// test.kt:1 E.component2_7eebsb_k$
// test.kt:24 box
// test.kt:9 E.copy_1tks5_k$
// test.kt:5 E
// test.kt:5 E
// test.kt:9 E.copy_1tks5_k$
