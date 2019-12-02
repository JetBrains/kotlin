// FULL_JDK

fun foo(): List<String> = TODO()

fun <T> ba(): List<T> = TODO()

fun bar() =
    try {
        foo().filter {
            // Lambda remains effectively unresolved
            it.length > 2
        }
    } finally {
        ba()
    }
