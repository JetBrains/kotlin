// "Make 'T' reified and 'dereferenceClass' inline" "true"

fun <T: Any> dereferenceClass(): Any =
        T::class<caret>
