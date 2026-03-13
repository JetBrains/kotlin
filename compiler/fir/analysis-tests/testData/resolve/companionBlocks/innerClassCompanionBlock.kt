// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    inner class Inner {
        companion {
            fun foo() {
                foo()
            }
        }

        fun test() {
            foo()
        }
    }

    fun test() {
        Inner.foo()
    }
}

fun test() {
    C.Inner.foo()
}

companion fun C.Inner.ext1() {
    foo()
}

companion fun C.ext2() {
    Inner.foo()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner */
