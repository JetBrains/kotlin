// constructor vs. fun overload

package constructorVsFun

class <!CONFLICTING_OVERLOADS!>a()<!> { }

<!CONFLICTING_OVERLOADS!>fun a()<!> = 1

class Tram {
    <!CONFLICTING_OVERLOADS!>fun f()<!> { }

    class <!CONFLICTING_OVERLOADS!>f()<!> { }
}

class Yvayva {
    companion object {
        <!CONFLICTING_OVERLOADS!>fun fghj()<!> { }

        class <!CONFLICTING_OVERLOADS!>fghj()<!> { }
    }
}

class Rtyu {
    fun ololo() { }

    companion object {
        class ololo() { }
    }
}

