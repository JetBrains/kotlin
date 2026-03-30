@Target(AnnotationTarget.TYPE)
annotation class Composable

fun Button(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {}

val textButton: (text: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) -> Unit = ::Button

fun Wrapper(...textButton.$callbacks) {
    <expr>onClick</expr>()
}
