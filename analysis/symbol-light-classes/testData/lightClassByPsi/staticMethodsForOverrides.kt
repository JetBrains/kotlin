// LIBRARY_PLATFORMS: JVM

interface Base {
    fun function()

    var property: Int
}

class ClassWithCompanion {
    companion object : Base {
        @JvmStatic
        override fun function() {}

        @JvmStatic
        override var property: Int
            get() = 1
            set(value) {}
    }
}

interface InterfaceWithCompanion {
    companion object : Base {
        @JvmStatic
        override fun function() {}

        @JvmStatic
        override var property: Int
            get() = 1
            set(value) {}
    }
}

interface InterfaceWithImplementations : Base {
    override fun function() {}

    override var property: Int
        get() = 1
        set(value) {}
}
