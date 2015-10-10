package test

import kotlin.platform.platformStatic

class PlatformStaticClass {
    companion object {
        @platformStatic
        fun <T> inClassObject() {}
    }

    fun <T> inClass() {}
}

