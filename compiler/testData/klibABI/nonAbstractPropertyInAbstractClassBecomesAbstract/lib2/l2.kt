package lib2

import lib1.A

class B : A() {
    override val baz1 = -42
    override val baz2 get() = -42

    val unlinkedPropertyUsage get() = foo1 + foo2 + bar1 + bar2
}

class B1 : A() {
    override val baz1 = -42
    override val baz2 get() = -42

    val unlinkedPropertyUsage = foo1
}

class B2 : A() {
    override val baz1 = -42
    override val baz2 get() = -42

    val unlinkedPropertyUsage = foo2
}

class B3 : A() {
    override val baz1 = -42
    override val baz2 get() = -42

    val unlinkedPropertyUsage = bar1
}

class B4 : A() {
    override val baz1 = -42
    override val baz2 get() = -42

    val unlinkedPropertyUsage = bar2
}
