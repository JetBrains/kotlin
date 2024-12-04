// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
import java.lang.Integer.getInteger

fun foo() {
    getInteger("text", 239)
}