// LIBRARY_PLATFORMS: JVM

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
// LIGHT_ELEMENTS_NO_DECLARATION: AbstractKotlinClass.class[Companion], KotlinClass.class[Companion]