import kotlin.js.json
import kotlin.js.Json
import kotlin.js.JSON
import kotlin.js.undefined

fun main(args: Array<String>) {
    val a: dynamic = undefined
    val b = JSON.stringify(a)
    val j: Json = json("a" to 1)
}

// For KT-3620 Don't auto-import kotlin.js.* and remove in `optimize imports`
