// LANGUAGE: +CompanionBlocks +CompanionExtensions

class Owner {
    companion {
        fun foo(): Int = 1
    }
}

fun bar(): Int = <expr>Owner.foo()</expr>
