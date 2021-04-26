// !LANGUAGE: +ReleaseCoroutines +ExperimentalBuilderInference
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// SKIP_TXT

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

@kotlin.coroutines.RestrictsSuspension
class RestrictedController<T> {
    suspend fun yield(x: T) {}

    suspend fun anotherYield(x: T) {
        yield(x)
        this.yield(x)

        yield2(x)
        this.yield2(x)

        with(this) {
            yield(x)
            this@with.yield(x)

            yield2(x)
            this@with.yield2(x)
        }
    }
}

fun <T> buildSequence(@BuilderInference c: suspend RestrictedController<T>.() -> Unit) {}

@BuilderInference
suspend fun <T> RestrictedController<T>.yield2(x: T) {}

fun test() {
    buildSequence<Int> a@{
        buildSequence<Int> b@{
            yield(1)
            yield2(1)
            this@b.yield(1)
            this@b.yield2(1)

            this@a.yield(2) // Should be error
            this@a.yield2(2) // Should be error

            with(this) {
                yield(3)
                this@with.yield(3)

                yield2(3)
                this@with.yield2(3)
            }
        }
    }

    buildSequence<Int> {
        buildSequence<String> {
            yield("a")
            yield2("a")
            this.yield("b")
            this.yield2("b")

            yield(1) // Should be error
            yield2(1) // Should be error

            with(this) {
                yield("")
                this@with.yield("")

                yield2("")
                this@with.yield2("")
            }
        }
    }

    buildSequence<Int> a@{
        yield(1)
        yield2(1)
        buildSequence {
            yield("")
            yield2("")
            this@a.yield(1)
            this@a.yield2(1)

            with(this) {
                yield("")
                this@with.<!UNRESOLVED_REFERENCE!>yield<!>("")

                yield2("")
                this@with.<!INAPPLICABLE_CANDIDATE!>yield2<!>("")
            }
        }
    }

    buildSequence<String> {
        yield("")
        RestrictedController<String>().yield("1")
    }
}
