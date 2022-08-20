package lib2

import lib1.A

class B : A {
    override val bar get() = -42

    val unlinkedPropertyUsage get() = foo
}

class B1 : A {
    override val bar get() = -42

    val unlinkedPropertyUsage = foo
}
