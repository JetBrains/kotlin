// SET_INT: CALL_PARAMETERS_WRAP = 4
// SET_TRUE: ALLOW_TRAILING_COMMA

fun foo() {
    testtest(foofoo, foofoo, foofoo,
            foofoo, bar)

    testtest(
            foofoo, foofoo, foofoo, foofoo, bar
    )

    testtest(foofoo, foofoo, foofoo, foofoo, bar
    )

    testtest(foofoo, foofoo, foofoo, foofoo,
            bar
    )

    testtest(foofoo
    )

    testtest(
            foofoo)

    testtest(
            foofoo
    )

    testtest(
            foofoo,
    )

    testtest(foofoo, testtest(testtest(foofoo)))

    testtest(
            foofoo, fososos, testtest(testtest(foofoo)),
    )

    testtest(foofoo,
            testtest(testtest(
                    foofoo,
            )),
            testsa)

    testtest(foofoo,
            seee,
            testtest(testtest(
                    foofoo,
            )),
            testsa)

    useCallable("A", Callable { println("Hello world") })

    useCallable("B", "C", Callable {
        println("Hello world")
    }, Callable {
        println("Hello world")
    })

    useCallable(Callable { println("Hello world") })

    useCallable(
            Callable { println("Hello world") },
    )

    useCallable(Callable { println("Hello world") }
    )

    useCallable(Callable { println("Hello world") }) {

    }

    useCallable(
            Callable { println("Hello world") })

    useCallable("A", { println("Hello world") })

    useCallable("B", "C", {
        println("Hello world")
    }, {
        println("Hello world")
    })

    useCallable({ println("Hello world") })

    useCallable(
            { println("Hello world") },
    )

    useCallable({ println("Hello world") }
    )

    useCallable({ println("Hello world") }) {

    }

    useCallable(
            { println("Hello world") })

    useCallable("A", foo() { println("Hello world") })

    useCallable("B", "C", foo() {
        println("Hello world")
    }, foo() {
        println("Hello world")
    })

    useCallable(foo() { println("Hello world") })

    useCallable(
            foo() { println("Hello world") },
    )

    useCallable(foo() { println("Hello world") }
    )

    useCallable(foo() { println("Hello world") }) {

    }

    useCallable(
            foo() { println("Hello world") })

    useCallable("A", object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    })

    useCallable("A", object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    })

    useCallable("B", "C", object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    }, foo() {
        println("Hello world")
    })

    useCallable(object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    })

    useCallable(
            object : Callable<Unit> {
                override fun call() {
                    println("Hello world")
                }
            },
    )

    useCallable(object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    }
    )

    useCallable(object : Callable<Unit> {
        override fun call() {
            println("Hello world")
        }
    }) {

    }

    useCallable(
            object : Callable<Unit> {
                override fun call() {
                    println("Hello world")
                }
            })

    testtest(
            foofoo, foofoo, foofoo, foofoo,
            bar /*
    */, /* */ foo
    )

    testtest(/*
    */foofoo, foofoo, foofoo, /*

    */
            foofoo, bar)

    testtest(foofoo, foofoo, foofoo, foofoo, bar/*
    */)

    testtest(foofoo, foofoo, foofoo, foofoo, bar // awdawda
    )

    testtest(foofoo, foofoo, foofoo, foofoo, /*

    */
            bar
    )

    testtest(foofoo // fd
    )

    testtest( /**/
            foofoo
    )

    testtest(
            foofoo,/**/
    )

    testtest(foofoo, foofoo, foofoo, foofoo/*
     */, /* */ bar
    )

    testtest(foofoo // fd
    )

    testtest( /**/
            foofoo
    )

    testtest(
            foofoo,/**/
    )

    testtest(
            foofoo, fososos,/*
    */
            testtest(testtest(foofoo)),
    )

    testtest(foofoo,
            testtest(testtest(
                    foofoo,
            )), /**/
            testsa)

    testtest(foofoo,
            testtest(testtest(
                    foofoo,
            ))/* */, /**/
            testsa)

    testtest(foofoo,
            testtest(testtest(
                    foofoo,
            ))/*
    */,
            testsa)

    testtest(foofoo,
            seee,
            testtest(testtest(
                    foofoo,
            )), /**/
            testsa)

    testtest(foofoo,
            seee,
            testtest(testtest(
                    foofoo,
            )), /*
    */
            testsa)

    useCallable("B", "C", Callable {
        println("Hello world")
    }, /* */ Callable {
        println("Hello world")
    })

    useCallable(Callable { println("Hello world") } // ffd
    )
}