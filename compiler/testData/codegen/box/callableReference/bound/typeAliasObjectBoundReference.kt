object Thing {
    fun something(thing: String) = thing
}

typealias ThingAlias = Thing

fun box() = "OK".run(ThingAlias::something)
