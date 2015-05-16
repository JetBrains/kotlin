// "Remove identifier from function expressions in the whole project" "true"

inline fun run(block: () -> Unit) = block()
annotation class ann

fun foo() {
    l2@ @ann l2@ fun local() {
        run(l1@ fun() { return@l1 })

        run(label@ expr@ fun() {
            return@label
            return@expr
        })
    }
}

class A {
    fun bar() {
        run(toRun@ fun() {
            if (1 == 1) return@toRun
            return@bar
        })

        run(
                /* abc */  fun /* cde */ () {
                    return@bar
                }
        )

        run(
                /* abc */
                foo@ fun /* cde */ () {
                    return@foo
                }
        )
    }

    init {
        (foo@ fun A.() {
            (fun() {
                val x = 1
            })
            return@foo
        })
    }
}
