// COMPILATION_ERRORS

fun noDeclarations() {
    val x = listOf(1, 2).map { it + 1 }
    if (x.isEmpty()) {
        // fun class object interface
        println("fun class object interface")
    }
    println(x::class)
}

fun classLiteralBeforeLocalClass() {
    val k = Int::class
    class AfterClassLiteral
}

fun localClassInLambda() {
    run {
        class Local
    }
}

fun localFunInterface() {
    fun interface I {
        fun f()
    }
}

fun objectLiteral() = run { object : Any() {} }

fun anonymousFunction() {
    listOf(1).forEach(fun(x: Int) {})
}

val lambdaInInitializer = { fun local() {} }

val property: Int
    get() {
        class InGetter
        return 0
    }

class Outer {
    init {
        object InInit
    }

    constructor() {
        fun inConstructor() {}
    }
}

fun deeplyNested() {
    if (true) {
        while (true) {
            try {
                class Deep
            } finally {
            }
        }
    }
}

fun inTemplate() {
    "${object {}}"
}
