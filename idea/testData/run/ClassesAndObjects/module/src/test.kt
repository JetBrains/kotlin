package q

import kotlin.platform.platformStatic

// RUN: q.Foo
object Foo {
    // RUN: q.Foo
    @platformStatic fun main(s: Array<String>) {
        println("Foo")
    }

    // RUN: q.Foo.InnerFoo
    class InnerFoo {
        companion object {
            // RUN: q.Foo.InnerFoo
            @platformStatic fun main(s: Array<String>) {
                println("InnerFoo")
            }
        }
    }

    // RUN: q.Foo
    class InnerFoo2 {
        // RUN: q.Foo
        @platformStatic fun main(s: Array<String>) {
            println("InnerFoo")
        }
    }
}

// RUN: q.TestKt
object Foo2 {
    // RUN: q.TestKt
    fun main(s: Array<String>) {
        println("Foo2")
    }
}

// RUN: q.Bar
class Bar {
    companion object {
        // RUN: q.Bar
        @platformStatic fun main(s: Array<String>) {
            println("Bar")
        }
    }
}

// RUN: q.TestKt
class Bar2 {
    companion object {
        // RUN: q.TestKt
        fun main(s: Array<String>) {
            println("Bar2")
        }
    }
}

// RUN: q.TestKt
class Baz {
    // RUN: q.TestKt
    platformStatic fun main(s: Array<String>) {
        println("Baz")
    }
}

// RUN: q.TestKt
class Baz2 {
    // RUN: q.TestKt
    fun main(s: Array<String>) {
        println("Baz2")
    }
}

// RUN: q.TestKt
fun main(s: Array<String>) {
    println("Top-level")
}