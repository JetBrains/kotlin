// FIR_IDENTICAL
// CHECK_TYPE

interface Predicate<T>

fun <T> Predicate(x: (T?) -> Boolean): Predicate<T> = null!!

fun foo() {
    process(Predicate {
        x -> x checkType { _<String?>() }

        true
    })
}

fun process(x: Predicate<String>) {}
