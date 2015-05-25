// WITH_RUNTIME
fun foo(list: List<String?>) {
    list.filter { it != null } /* filter out nulls */
        .forEach<caret> { print(1..4) /* print x */ }
}