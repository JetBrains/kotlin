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
    a<!UNSAFE_CALL!>.<!>b<!UNSAFE_CALL!>.<!>inc()
}

fun test2(a: A?){
    require(a?.e?.d != null)
    var k: C = <!TYPE_MISMATCH!>a<!UNSAFE_CALL!>.<!>e<!>
    var k2: Int = <!TYPE_MISMATCH!>a<!UNSAFE_CALL!>.<!>e<!UNSAFE_CALL!>.<!>d<!>
    var k3: Int? = a<!UNSAFE_CALL!>.<!>b
}

fun test3(a:A?){
    val t = (a?.b != null)
    require(t)
    a<!UNSAFE_CALL!>.<!>b<!UNSAFE_CALL!>.<!>inc()
}