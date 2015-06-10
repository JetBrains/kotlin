import kotlin.platform.platformStatic
class A {
    platformStatic <!PLATFORM_STATIC_ILLEGAL_USAGE!>constructor()<!> {}
    inner class B {
        platformStatic <!PLATFORM_STATIC_ILLEGAL_USAGE!>constructor()<!> {}
    }
}

class C platformStatic <!PLATFORM_STATIC_ILLEGAL_USAGE!>constructor()<!>