// FIR_COMPARISON
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
                <caret>
            }
        }
    }
}

// EXIST: {"lookupString":"otherThing", "attributes": ["bold"]}
// EXIST: {"lookupString":"child", "attributes": ["bold"]}
// EXIST: {"lookupString":"otherChild", "attributes": ["bold"]}
