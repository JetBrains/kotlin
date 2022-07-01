// constructor vs. fun overload

package constructorVsFun

class a<!CONFLICTING_OVERLOADS!>()<!> { }

<!CONFLICTING_OVERLOADS!>fun a()<!> = 1

class Tram {
    fun f() { }

    class f() { }
}

class Yvayva {
    companion object {
        fun fghj() { }

        class fghj() { }
    }
}

class Rtyu {
    fun ololo() { }

    companion object {
        class ololo() { }
    }
}