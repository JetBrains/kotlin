// !DIAGNOSTICS: -UNUSED_PARAMETER

trait A {
    val prop: Int
}

class AImpl: A  {
    override val <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>prop<!> by Delegate()
}

fun foo() {
    AImpl().prop
}

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): String {
        return ""
    }
}
