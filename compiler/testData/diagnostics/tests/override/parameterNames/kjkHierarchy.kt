// FILE: Super.kt

interface Super {
    fun foo(superName: Int)
}

// FILE: Sub.java

interface Sub extends Super {
}

// FILE: SubSub.kt

class SubSub : Sub {
    override fun foo(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>subName<!>: Int) {}
}
