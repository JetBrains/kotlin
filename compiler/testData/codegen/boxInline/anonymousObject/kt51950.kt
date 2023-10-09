// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
interface Service {
    fun send(message: String): String
}

inline fun Service.decorate(crossinline decorate: (Service) -> Service): Service =
    object : Service by decorate(this) {}

inline fun Service.transformMessage(crossinline transform: (String) -> String) =
    decorate { service ->
        object : Service {
            override fun send(message: String): String =
                service.send(transform(message))
        }
    }

// FILE: 2.kt
fun Service.append(suffix: String): Service =
    transformMessage { it + suffix }

fun box(): String =
    object : Service {
        override fun send(message: String): String = message
    }.append("K").send("O")
