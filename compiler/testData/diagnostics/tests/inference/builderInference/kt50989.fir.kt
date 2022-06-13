// WITH_STDLIB

fun main() {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(get(0))
    }
}