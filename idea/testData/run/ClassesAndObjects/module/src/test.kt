package q

import kotlin.platform.platformStatic

// RUN: q.Foo
object Foo {
    // RUN: q.Foo
    platformStatic fun main(s: Array<String>) {
        println("Foo")
    }

    // RUN: q.Foo.InnerFoo
    class InnerFoo {
        default object {
            // RUN: q.Foo.InnerFoo
            platformStatic fun main(s: Array<String>) {
                println("InnerFoo")
            }
        }
    }

    // RUN: q.Foo
    class InnerFoo2 {
        // RUN: q.Foo
        platformStatic fun main(s: Array<String>) {
            println("InnerFoo")
        }
    }
}

// RUN: q.QPackage
object Foo2 {
    // RUN: q.QPackage
    fun main(s: Array<String>) {
        println("Foo2")
    }
}

// RUN: q.Bar
class Bar {
    default object {
        // RUN: q.Bar
        platformStatic fun main(s: Array<String>) {
            println("Bar")
        }
    }
}

// RUN: q.QPackage
class Bar2 {
    default object {
        // RUN: q.QPackage
        fun main(s: Array<String>) {
            println("Bar2")
        }
    }
}

// RUN: q.QPackage
class Baz {
    // RUN: q.QPackage
    platformStatic fun main(s: Array<String>) {
        println("Baz")
    }
}

// RUN: q.QPackage
class Baz2 {
    // RUN: q.QPackage
    fun main(s: Array<String>) {
        println("Baz2")
    }
}

// RUN: q.QPackage
fun main(s: Array<String>) {
    println("Top-level")
}