// FIR_COMPARISON
fun main(args: Array<String>) {
    args.filter<caret> {it != ""}
}

// ELEMENT: filterNot