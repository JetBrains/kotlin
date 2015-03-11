package d

trait T

class A(i: Int) {}

val c = 0

fun g(a: A) {}

fun A.ext()

object O1 {
    fun f() {
    }
}

object O2 {
}

enum class E {
    ENTRY
}

class Outer {
    inner class Inner {
    }
    class Nested {
    }
    enum class NestedEnum {
    }
    object NestedObj {
    }
    trait NestedTrait {
    }
    annotation class NestedAnnotation
}

class ClassObject {
    default object {
    }
}