// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

package script.long.name.inside.packag

interface I {
    fun g(): I
}

fun f(): I {
    return object : I {
        override fun g() = object : I {
            override fun g(): I = this

            override fun toString() = "OK"
        }
    }
}

val rv = f().g().g().g()

// expected: rv: OK