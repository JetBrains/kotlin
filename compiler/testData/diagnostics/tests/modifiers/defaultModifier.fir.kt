companion class A {
    companion object {

    }
}

class B {
    companion object

    val c: Int = 1
}

class C {
    companion object A {

    }
}

class D {
    companion object A {
        companion object {
        }
    }
}

companion object G {
    companion object
}

companion interface H {
    companion object
}

class J {
    companion object C {
        companion object
    }
}

companion enum class Enum {
    E1,
    E2;

    companion object
}

companion fun main() {

}

companion var prop: Int = 1
    companion get
    companion set

class Z(companion val c: Int)