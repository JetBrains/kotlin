class A {
    companion object {

    }
}

class A {
    companion object

    val c: Int = 1
}

class B {
    public companion object A {

    }
}

class B {
    companion object A {
        companion object {
        }
    }
}

companion object B
//should be error
companion object {

}

object A {
    companion object
}

interface A {
    companion object

    class C {
        companion object C {
            companion object
        }
    }
}

enum class D {
    A, B;

    companion object
}


//should be error
class A {
    class companion object
}

class A {
    companion public final object
}

//should be error
companion class {}

//should be error
val t = companion object {

}

enum class I {
    A,
    B;

    companion object
}

enum class I {
    A,
    B;

    companion object {}
}