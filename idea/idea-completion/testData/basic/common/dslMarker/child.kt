@DslMarker
annotation class SimpleDsl

@SimpleDsl
class DslRoot {
    fun container(body: DslContainer.() -> Unit) {
        body(DslContainer())
    }
}

@SimpleDsl
class DslContainer {
    fun child(body: DslChild.() -> Unit) {
        body(DslChild())
    }
}

@SimpleDsl
class DslChild {
    operator fun String.unaryMinus() {
        println(this)
    }
}

fun dsl(body: DslRoot.() -> Unit) {
    body(DslRoot())
}

fun test() {
    dsl {
        container {
            child {
                -"Some"
                <caret>
            }
        }
    }
}

// ABSENT: container
// ABSENT: lookupString