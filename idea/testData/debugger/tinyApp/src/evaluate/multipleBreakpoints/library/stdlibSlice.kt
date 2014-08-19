package stdlibSlice

fun main(args: Array<String>) {
    val c: CharSequence = "CharSequence"
    c.slice(0..1)
}

// ADDITIONAL_BREAKPOINT: StringsJVM.kt:CharSequence.slice(range: IntRange): CharSequence

// EXPRESSION: range.start
// RESULT: 0: I