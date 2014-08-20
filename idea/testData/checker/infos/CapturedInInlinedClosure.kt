inline fun <T> run(f: () -> T) = f()
fun run2(f: () -> Unit) = f()

fun inline() {
    val x = 1
    run { x }

    val x1 = 1
    run ({ x1 })

    val x2 = 1
    run (f = { x2 })

    val x3 = 1
    run {
        run {
            x3
        }
    }
}

fun notInline() {
    val <info descr="Value captured in a closure">y2</info> = 1
    run { <info descr="Value captured in a closure">y2</info> }
    run2 { <warning><info descr="Value captured in a closure">y2</info></warning> }

    val <info descr="Value captured in a closure">y3</info> = 1
    run2 { <warning><info descr="Value captured in a closure">y3</info></warning> }
    run { <info descr="Value captured in a closure">y3</info> }

    // wrapped, using in not inline
    val <info descr="Value captured in a closure">z</info> = 2
    { <info descr="Value captured in a closure">z</info> }()

    val <info descr="Value captured in a closure">z1</info> = 3
    run2 { <warning><info descr="Value captured in a closure">z1</info></warning> }
}

fun nestedDifferent() { // inline within non-inline and vice-versa
    val <info descr="Value captured in a closure">y</info> = 1
    {
        run {
            <info descr="Value captured in a closure">y</info>
        }
    }()

    val <info descr="Value captured in a closure">y1</info> = 1
    run {
        { <info descr="Value captured in a closure">y1</info> }()
    }
}

fun localFunctionAndClass() {
    val <info descr="Value captured in a closure">u</info> = 1
    fun localFun() {
        run {
            <info descr="Value captured in a closure">u</info>
        }
    }

    val <info descr="Value captured in a closure">v</info> = 1
    class LocalClass {
        fun f() {
            run {
                <info descr="Value captured in a closure">v</info>
            }
        }
    }
}

fun objectExpression() {
    val <info descr="Value captured in a closure">u1</info> = 1
    object : Any() {
        fun f() {
            run {
                <info descr="Value captured in a closure">u1</info>
            }
        }
    }

    val <info descr="Value captured in a closure">u2</info> = 1
    object : Any() {
        val <info>prop</info> = run {
            <info descr="Value captured in a closure">u2</info>
        }
    }

    val <info descr="Value captured in a closure">u3</info> = ""
    object : Throwable(run { <info descr="Value captured in a closure">u3</info> }) {
    }
}