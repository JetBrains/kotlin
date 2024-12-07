// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-49283

// IGNORE_BACKEND_K1: ANY

fun takeListOfStrings(x: List<String>) {}

fun box(): String {
    val result = buildList l1@ { // Not enough information to infer type variable E, but could be inferred into String
        val anotherList = buildList {
            takeListOfStrings(this)
            this@l1.add("OK")
        }
    }

    return result[0]
}
