fun bool_yes(): Boolean = true

fun main(args: Array<String>) {
    if (!bool_yes()) throw Error()
}
