interface Maybe<T>
class Some<T>(val value: T) : Maybe<T>
class None<T> : Maybe<T>

fun <T> none() : None<T> = TODO()

fun test1() : Maybe<String?> = if (true) none() else Some("")

fun test2() : Maybe<String?> = when {
    true -> none()
    else -> Some("")
}

fun test3() : Maybe<String?> = when {
    true -> none()
    else -> Some<String?>("")
}

fun test4() : Maybe<String?> {
    when ("") {
        "a" -> return none()
        else -> return Some<String?>("")
    }
}