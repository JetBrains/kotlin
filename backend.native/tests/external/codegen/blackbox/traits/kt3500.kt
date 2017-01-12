// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

interface BK {
    fun foo(): String = 10.toString()
}

interface KTrait: BK {
    override fun foo() = 30.toString()
}

class A : BK, KTrait {

}

fun box(): String {
    return if (A().foo() == "30") "OK" else "fail"
}