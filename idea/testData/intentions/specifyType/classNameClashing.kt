// WITH_RUNTIME

fun getEntry() : Map.Entry<kotlin.Array<String>, java.sql.Array> {
    throw Error()
}

val <caret>x = getEntry()