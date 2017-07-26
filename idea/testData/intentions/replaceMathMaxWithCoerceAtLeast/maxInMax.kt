// WITH_RUNTIME
import java.lang.Math.max

fun foo() {
    Math.max(max(1, 3)<caret>, max(2, 4))
}