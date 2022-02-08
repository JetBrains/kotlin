// !CHECK_TYPE
// See also KT-10896: Wrong inference of if / else result type

interface Option<T>
class Some<T> : Option<T>
class None<T> : Option<T>

fun <T> bind(r: Option<T>): Option<T> {
    return if (r is Some) {
        // Ideally we should infer Option<T> here (see KT-10896)
        (if (true) None() else r) checkType { _<Option<T>>() }
        // Works correctly
        if (true) None() else r
    }
    else r
}

fun <T> bind2(r: Option<T>): Option<T> {
    return if (r is Some) {
        // Works correctly
        if (true) None<T>() else r
    }
    else r
}

fun <T, R> bind3(r: Option<T>): Option<T> {
    return if (r is Some) <!TYPE_MISMATCH!>{
        // Diagnoses an error correctly
        if (true) None<R>() else r
    }<!>
    else r
}

fun <T> bindWhen(r: Option<T>): Option<T> {
    return when (r) {
        is Some -> {
            // Works correctly
            if (true) None() else r
        }
        else -> r
    }
}

interface SimpleOption
class SimpleSome : SimpleOption
class SimpleNone : SimpleOption

fun bindNoGeneric(r: SimpleOption): SimpleOption {
    return if (r is SimpleSome) {
        (if (true) SimpleNone() else r) checkType { _<SimpleOption>() }
        if (true) SimpleNone() else r
    }
    else r
}
