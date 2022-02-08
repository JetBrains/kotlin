fun <T> materialize(): T = TODO()

fun main() {
    if ("" == materialize()) return // FE1.0: OK, type argument inferred to Any?
    if (materialize() == "") return // FE1.0: Error, uninferred type argument for `T`
}
