// !LANGUAGE: +InnerClassInEnumEntryClass
inner fun foo() {}
inner val prop = 42

inner class A
inner interface B
inner object C

class D {
    inner class E
    inner interface F
    inner object G
    inner enum class R
    inner annotation class S
    inner companion object
}

enum class H {
    I0 {
        inner class II0
    },
    inner I {
        inner class II
    };
    
    inner class J
}

interface K {
    inner class L
}

object N {
    inner class O
}

class P {
    companion object {
        inner class Q
    }
}

val R = object {
    inner class S
}
