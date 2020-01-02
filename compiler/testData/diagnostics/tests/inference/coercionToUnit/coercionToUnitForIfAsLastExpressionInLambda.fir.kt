// !LANGUAGE: +NewInference

class Obj

fun foo(): String? {
    run {
        if (true) return@run

        if (true) Obj()
    }

    run {
        if (true) return@run

        if (true) return Obj() // correct error, type check against return type of function "foo"
    }

    run {
        if (true)
            return@run
        else
            if (true) 42
    }

    run {
        if (true)
            42
        else
            if (true) 42
    }

    run {
        if (true) return@run

        if (true) {
            Obj()
        } else
            if (true) return null
    }

    return ""
}