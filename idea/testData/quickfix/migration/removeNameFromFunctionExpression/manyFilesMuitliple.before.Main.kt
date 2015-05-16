// "Remove identifier from function expressions in the whole project" "true"

inline fun run(block: () -> Unit) = block()
annotation class ann

fun foo() {
    l2@ @ann l2@ fun local() {
        run(l1@ fun() { return@l1 })

        run(label@ fun expr<caret>() {
            return@label
            return@expr
        })
    }
}

class A {
    fun bar() {
        run(fun toRun() {
            if (1 == 1) return@toRun
            return@bar
        })

        run(
                /* abc */  fun /* cde */ toRun() {
                    return@bar
                }
        )

        run(
                /* abc */
                fun /* cde */ foo() {
                    return@foo
                }
        )
    }

    init {
        (fun A.foo() {
            (fun bar() {
                val x = 1
            })
            return@foo
        })
    }
}
