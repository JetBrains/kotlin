package test

import kotlin.platform.platformStatic

class PlatformStaticClass {
    companion object {
        @platformStatic
        fun inClassObject<T>() {}
    }

    fun inClass<T>() {}
}

