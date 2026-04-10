// LANGUAGE: +CompanionBlocksAndExtensions
class C {
    companion {
        fun foo() = "O"
    }
}

companion val C.bar = "K"

val result = C.foo() + C.bar

// expected: result: OK
