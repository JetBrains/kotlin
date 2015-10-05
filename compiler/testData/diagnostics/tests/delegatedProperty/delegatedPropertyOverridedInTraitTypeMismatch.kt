// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A {
    val prop: Int
}

class AImpl: A  {
    override val <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>prop<!> by Delegate()
}

fun foo() {
    AImpl().prop
}

class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): String {
        return ""
    }
}
