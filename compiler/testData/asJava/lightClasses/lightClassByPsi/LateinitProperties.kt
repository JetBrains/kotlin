// !GENERATE_PROPERTY_ANNOTATIONS_METHODS
class KotlinClass {
    lateinit var classLateinitVariable: Custom

    companion object {
        lateinit var companionLateinitVariable: Custom

        @JvmStatic
        lateinit var companionLateinitStaticVariable: Custom
    }
}

abstract class AbstractKotlinClass {
    lateinit var classLateinitVariable: Custom

    companion object {
        lateinit var companionLateinitVariable: Custom

        @JvmStatic
        lateinit var companionLateinitStaticVariable: Custom
    }
}

lateinit var topLevelLateinit: Custom

class Custom