interface Foo

class Bar(f: Foo) : Foo by f {
    val <!REDECLARATION!>`$$delegate_0`<!>: Foo? = null
}
