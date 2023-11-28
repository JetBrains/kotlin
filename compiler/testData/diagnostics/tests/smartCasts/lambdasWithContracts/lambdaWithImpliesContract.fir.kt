// ISSUE: KT-37308
// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun CharSequence?.valueIsNotNull(): Boolean {
    contract {
        returns(true) implies (this@valueIsNotNull != null)
    }
    return this != null
}
@OptIn(ExperimentalContracts::class)
fun CharSequence?.valueIsNull(): Boolean {
    contract {
        returns(false) implies (this@valueIsNull != null)
    }
    return this == null
}

class A {
    val b: String? = ""
    val e: C? = C()
}

class C {
    val d: String? = ""
}

fun test1(a: A?) {
    if (!a?.b.valueIsNull()) {
        a.b.length
    }
}

fun test2(a: A?) {
    require(!a?.b.valueIsNull())
    a.b.length
}

fun test3(a: A?) {
    if(a?.b.valueIsNotNull()){
        a.b.length
    }
}

fun test4(a :A?) {
    require(a?.b.valueIsNotNull())
    a.b.length
}

fun test5(a :A?) {
    require(a?.e?.d.valueIsNotNull())
    a.e.d.length
}