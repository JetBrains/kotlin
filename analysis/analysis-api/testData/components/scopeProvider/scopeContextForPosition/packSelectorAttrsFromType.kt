@Target(AnnotationTarget.TYPE)
annotation class Composable

class ButtonProps(
    val text: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val content: @Composable () -> Unit,
)

fun Wrapper(...ButtonProps.$attrs) {
    <expr>text</expr>
    enabled.not()
}
