// METADATA_TARGET_PLATFORMS: Native, JS
// MODULE: lib
interface A {
    @Throws(Exception::class)
    fun foo()
}

// MODULE: app(lib)
class B : A {
    <!INCOMPATIBLE_THROWS_OVERRIDE!>@Throws(Exception::class)<!>
    override fun foo() {}
}
