// WITH_STDLIB

suspend inline fun simple() {}

suspend inline fun <T> generic() {}

suspend inline fun <T, reified U> genericWithReified() {}

suspend inline fun Unit.simple() {}

suspend inline fun <T> T.generic() {}

suspend inline fun <T, reified U> T.genericWithReified() {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
suspend inline fun shouldNotHaveSuffix() {}

suspend inline fun acceptsCrossinline(crossinline c: () -> Unit) {}

private suspend inline fun privateInline() {}

class Foo {
    suspend inline fun simple() {}

    suspend inline fun <T> generic() {}

    suspend inline fun <T, reified U> genericWithReified() {}

    suspend inline fun Unit.simple() {}

    suspend inline fun <T> T.generic() {}

    suspend inline fun <T, reified U> T.genericWithReified() {}

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    suspend inline fun shouldNotHaveSuffix() {}

    suspend inline fun acceptsCrossinline(crossinline c: () -> Unit) {}

    private suspend inline fun privateInline() {}
}