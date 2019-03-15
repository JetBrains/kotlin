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

    fun otherChild(body: DslOtherChild.() -> Unit) {
        body(DslOtherChild())
    }

    fun anotherChild(body: DslAnotherChild.() -> Unit) {
        body(DslAnotherChild())
    }
}

@SimpleDsl
class DslChild {
    operator fun String.unaryMinus() {
        println(this)
    }
}

@DslMarker
annotation class SimpleOtherDsl

@SimpleOtherDsl
class DslOtherChild {
    fun otherThing() {

    }
}

@SimpleOtherDsl @SimpleDsl
class DslAnotherChild {
     fun foo () {

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

            }
            otherChild {
                otherThing()
                anotherChild {
                    <caret>
                }
            }
        }
    }
}

// EXIST: foo
// ABSENT: otherThing
// ABSENT: child
// ABSENT: otherChild
