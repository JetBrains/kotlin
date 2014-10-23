package test

import kotlin.platform.platformStatic

class PlatformStaticClass {
    class object {
        platformStatic
        fun inClassObject<T>() {}
    }

    fun inClass<T>() {}
}

