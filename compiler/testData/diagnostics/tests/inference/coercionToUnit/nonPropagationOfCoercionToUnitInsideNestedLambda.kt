// !LANGUAGE: +NewInference

class Obj

fun foo(): String? {
    run {
        if (true) return@run

        run {
            if (true) {
                Obj()
            } else
                <!INVALID_IF_AS_EXPRESSION!>if<!> (true) return null // Error, coercion to Unit doesn't propagate inside nested lambdas
        }

        if (true) {
            Obj()
        } else
            if (true) return null // OK, no error
    }

    run {
        if (true) return@run

        run {
            if (true) {
                Obj()
            } else
            <!INVALID_IF_AS_EXPRESSION!>if<!> (true) return null // Error, coercion to Unit doesn't propagate inside nested lambdas
        }
    }

    run {
        if (true) return@run

        run nestedRun@{
            if (true) return@nestedRun

            if (true) {
                Obj()
            } else
                if (true) return null // OK, additional empty labeled return helps
        }

        if (true) {
            Obj()
        } else
            if (true) return null // OK, no error
    }

    return ""
}