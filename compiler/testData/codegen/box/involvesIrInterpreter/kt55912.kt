public annotation class Entity(val foreignKeys: Array<String>)

@Entity(foreignKeys = kotlin.arrayOf("id")) // works without "kotlin."
class Record

fun box(): String {
    return "OK"
}