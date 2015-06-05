import kotlin.platform.platformStatic
class A {
    <!PLATFORM_STATIC_ILLEGAL_USAGE!>platformStatic constructor()<!> {}
    inner class B {
        <!PLATFORM_STATIC_ILLEGAL_USAGE!>platformStatic constructor()<!> {}
    }
}

class C platformStatic <!PLATFORM_STATIC_ILLEGAL_USAGE!>constructor()<!>
