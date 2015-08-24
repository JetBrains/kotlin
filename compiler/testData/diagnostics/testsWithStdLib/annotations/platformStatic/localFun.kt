// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.jvmStatic

fun main(args: Array<String>) {
    <!JVM_STATIC_NOT_IN_OBJECT!>@jvmStatic fun a()<!>{

    }
}