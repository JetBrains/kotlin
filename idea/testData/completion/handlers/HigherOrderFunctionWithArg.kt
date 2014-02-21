fun <T> Array<T>.filter(predicate : (T) -> Boolean) : java.util.List<T> = throw UnsupportedOperationException()

fun <T> Array<T>.filterNot(predicate : (T) -> Boolean) : java.util.List<T> = throw UnsupportedOperationException()

fun main(args: Array<String>) {
    args.filter<caret> {it != ""}
}
