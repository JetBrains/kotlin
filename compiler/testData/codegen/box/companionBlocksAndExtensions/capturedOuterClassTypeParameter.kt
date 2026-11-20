// LANGUAGE: +CompanionBlocks

class C {
    companion {
        fun <T> local(t: T): T {
            class Outer<R>(r: R) {
                inner class Inner(val r: R)
                val p: Inner = Inner(r)
            }

            return Outer(t).p.r
        }
    }
}

fun box() = C.local("OK")
