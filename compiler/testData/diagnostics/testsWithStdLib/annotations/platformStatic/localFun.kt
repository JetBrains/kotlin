// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.JvmStatic

fun main(args: Array<String>) {
    <!JVM_STATIC_NOT_IN_OBJECT!>@JvmStatic fun a()<!>{

    }
}