// !CHECK_TYPE

interface Predicate<T>

fun <T> Predicate(<!UNUSED_PARAMETER!>x<!>: (T?) -> Boolean): Predicate<T> = null!!

fun foo() {
    process(Predicate {
        x -> x checkType { _<String?>() }

        true
    })
}

fun process(<!UNUSED_PARAMETER!>x<!>: Predicate<String>) {}