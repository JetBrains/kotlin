// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Owner {
    companion {
        fun foo(p: Int): Int {
            val local = p
            class Local {
                fun localMember() {}
            }
            return local
        }

        val prop: Int
            get() = 1
    }

    companion object {
        fun bar() {}
    }
}

interface I {
    companion {
        fun baz() {}
    }
}

companion fun Owner.ext(): Int = 1

fun test() {
    val o = object {
        companion {
            fun anon() {}
        }
    }
}
