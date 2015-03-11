package test

import kotlin.platform.platformStatic

class PlatformStaticClass {
    default object {
        platformStatic
        fun inClassObject<T>() {}
    }

    fun inClass<T>() {}
}

