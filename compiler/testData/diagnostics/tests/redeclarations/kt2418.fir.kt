//KT-2418 Front-end allows enum constants with same name

package kt2418

enum class A {
    FOO,
    FOO
}

enum class B {
    FOO;
    
    fun FOO() {}
}

enum class C {
    FOO;
    
    val FOO = 1
}

enum class D {
    FOO;
    
    class FOO {}
}

