// IGNORE_BACKEND: WASM
// WASM test infra can't handle `EVALUATED` diagnostic

public annotation class Entity(val foreignKeys: Array<String>)

@Entity(foreignKeys = kotlin.arrayOf(<!EVALUATED("id")!>"id"<!>)) // works without "kotlin."
class Record

fun box(): String {
    return "OK"
}
