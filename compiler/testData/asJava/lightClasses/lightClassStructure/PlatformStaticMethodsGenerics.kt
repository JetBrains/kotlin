package test

class PlatformStaticClass {
    companion object {
        @JvmStatic
        fun <T> inClassObject() {}
    }

    fun <T> inClass() {}
}

