// ISSUE: KT-54443
// WITH_STDLIB

class A {
    val b: Int? = 1
    val e: C? = C()
}

class C {
    val d: Int? = 2
}

fun test(a: A?) {
    require(a?.b != null)
    a.b.inc()
}

fun test2(a: A?){
    require(a?.e?.d != null)
    var k: C = a.e
    var k2: Int = a.e.d
    var k3: Int? = a.b
}

fun test3(a:A?){
    val t = (a?.b != null)
    require(t)
    a.b.inc()
}