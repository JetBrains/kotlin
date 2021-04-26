// !LANGUAGE: +DefinitelyNotNullTypeParameters

interface A<T> {
    fun foo(x: T): T
    fun bar(x: T!!): T!!
}

interface B<T1> : A<T1> {
    override fun foo(x: T1): T1
    override fun bar(x: T1!!): T1!!
}

interface C<T2> : A<T2> {
    override fun foo(x: T2!!): T2!!
    override fun bar(x: T2): T2
}

interface D : A<String?> {
    override fun foo(x: String?): String?
    override fun bar(x: String): String
}

interface E : A<String> {
    override fun foo(x: String): String
    override fun bar(x: String): String
}

interface F : A<String?> {
    override fun foo(x: String): String
    override fun bar(x: String?): String?
}

interface G<T3 : Any> : A<T3> {
    override fun foo(x: T3): T3
    override fun bar(x: T3): T3
}
