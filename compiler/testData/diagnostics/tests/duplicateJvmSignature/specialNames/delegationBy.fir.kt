// FIR_DISABLE_LAZY_RESOLVE_CHECKS
interface Foo

class Bar(f: Foo) : Foo by f {
    val `$$delegate_0`: Foo? = null
}