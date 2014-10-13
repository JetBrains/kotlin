// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.platform.platformStatic

fun main(args: Array<String>) {
    <!PLATFORM_STATIC_NOT_IN_OBJECT!>[platformStatic] fun a()<!>{

    }
}