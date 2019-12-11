// FILE: Super.kt

interface Super {
    fun foo(superName: Int)
}

// FILE: Sub.java

interface Sub extends Super {
}

// FILE: SubSub.kt

class SubSub : Sub {
    override fun foo(subName: Int) {}
}
