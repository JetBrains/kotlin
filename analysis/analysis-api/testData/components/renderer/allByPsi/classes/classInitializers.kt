// RENDER_CLASS_INITIALIZERS
class WithInitializers(val size: Int) {
    val name = "name"

    init {
        require(size > 0)
    }

    init {
        println(name)
    }
}

object ObjectWithInitializer {
    init {
        println("created")
    }
}
