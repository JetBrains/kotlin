// WITH_STDLIB

annotation class StringRes
annotation class LongRes
annotation class DefRes

class ASimpleClass {
    @JvmOverloads
    fun String.showSnackbar(@StringRes stringResId: Int, duration: Int = 2) {

    }

    @JvmOverloads
    fun showSnackbarNoExtension(@StringRes stringResId: Int, duration: Int = 2) {

    }

    @JvmOverloads
    fun String.showSnackbarLong(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

    }

    @JvmOverloads
    fun showSnackbarLongNoExtension(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

    }
}

object BSimpleObject {
    @JvmStatic
    @JvmOverloads
    fun String.showSnackbar(@StringRes stringResId: Int, duration: Int = 2) {

    }

    @JvmStatic
    @JvmOverloads
    fun showSnackbarNoExtension(@StringRes stringResId: Int, duration: Int = 2) {

    }

    @JvmStatic
    @JvmOverloads
    fun String.showSnackbarLong(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

    }

    @JvmStatic
    @JvmOverloads
    fun showSnackbarLongNoExtension(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

    }
}

class CClassWithCompanion {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun String.showSnackbar(@StringRes stringResId: Int, duration: Int = 2) {

        }

        @JvmStatic
        @JvmOverloads
        fun showSnackbarNoExtension(@StringRes stringResId: Int, duration: Int = 2) {

        }

        @JvmStatic
        @JvmOverloads
        fun String.showSnackbarLong(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

        }

        @JvmStatic
        @JvmOverloads
        fun showSnackbarLongNoExtension(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

        }
    }
}

class DClassConstuctors {
    @JvmOverloads
    constructor(@StringRes stringResId: Int, duration: Int = 2) {

    }

    @JvmOverloads
    constructor(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

    }

    inner class InnerClass {
        @JvmOverloads
        constructor(@StringRes stringResId: Int, duration: Int = 2) {

        }

        @JvmOverloads
        constructor(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

        }

    }
}



@JvmOverloads
fun String.showSnackbar(@StringRes stringResId: Int, duration: Int = 2) {

}

@JvmOverloads
fun showSnackbarNoExtension(@StringRes stringResId: Int, duration: Int = 2) {

}

@JvmOverloads
fun String.showSnackbarLong(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

}


@JvmOverloads
fun showSnackbarLongNoExtension(@StringRes stringResId: Int, @DefRes duration: Int = 2, @LongRes oneMoreNonDefault: Long, andDefaultOne: String = "Default") {

}
