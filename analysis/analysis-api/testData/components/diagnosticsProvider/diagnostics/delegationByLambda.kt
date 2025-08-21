import java.io.Serializable

fun handleJob(handler: () -> Unit): Serializable {
    return ""
}

class FooBarJobHandler : Serializable by handleJob({
    unresolved
})


interface I

fun createI(s: String): I = null!!

class Foo() : I by { createI() }.invoke()