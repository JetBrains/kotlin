import kotlin.reflect.KClass

val javaClass: Class<String> = String::class.java
val kotlinClass: KClass<String> = String::class

fun foo() {
    val stringClass = String::class.java
    val arrayStringClass = Array<String>::class.java
}

