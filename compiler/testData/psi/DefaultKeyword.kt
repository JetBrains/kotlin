class A {
    default object {

    }
}

class A {
    default object

    val c: Int = 1
}

class B {
    public default object A {

    }
}

class B {
    default object A {
        default object {
        }
    }
}

default object B
//should be error
default object {

}

object A {
    default object
}

trait A {
    default object

    class C {
        default object C {
            default object
        }
    }
}

enum class D {
    A B

    default object
}

class A {
    default class object
}

//should be error
class A {
    class default object
}

class A {
    default public final object
}

//should be error
default class {}

//should be error
val t = default object {

}

enum class I {
    A
    B

    default object
}

enum class I {
    A
    B

    default object {}
}