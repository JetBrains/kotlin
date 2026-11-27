// WITH_STDLIB
// FULL_JDK

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: expressions, built-in-types-and-their-semantics, kotlin.nothing-1 -> paragraph 1 -> sentence 1
 * NUMBER: 9
 * DESCRIPTION: check kotlin.Nothing type
 */

fun box(): String {
    try {
        val city = City()
    } catch (e: NotImplementedError) {
        val city = City("", 1)

        city.country
        try {
            city.lakeConsumer()
        } catch (e: NotImplementedError) {
            try {
                city.streetsConsumer.stream().forEach { street -> street.invoke() }
            } catch (e: IllegalArgumentException) {
                try {
                    val city = City("", 2) { "lake" }
                    city.streetsConsumer[3].invoke()
                } catch (e: UnsupportedOperationException) {
                    return "OK"
                }
            }
        }
    }
    return "NOK"
}

class City(val name: String = "", val index: Any = TODO()) {
    var country: String = "Rus"
    var lakeConsumer: () -> String
    var streetsConsumer: MutableList<() -> String>

    init {
        lakeConsumer = { TODO() }
        streetsConsumer = mutableListOf({ "x" }, { "y" }, { throw IllegalArgumentException() })
    }

    constructor(name: String, index: Any, lakeConsumer: () -> String) : this(name, index) {
        this.lakeConsumer = lakeConsumer
        this.streetsConsumer.add { throw UnsupportedOperationException() }
    }
}
