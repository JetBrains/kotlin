package reflect

import kotlin.reflect.jvm.kotlinFunction

fun main() {
    String::class.annotations
    KotlinVersion::class.java.methods.first().kotlinFunction
}
