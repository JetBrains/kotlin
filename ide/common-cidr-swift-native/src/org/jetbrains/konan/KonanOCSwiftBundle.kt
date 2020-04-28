package org.jetbrains.konan

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

class KonanOCSwiftBundle private constructor() : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "messages.KonanOCSwiftBundle"

        @JvmStatic
        private val INSTANCE = KonanOCSwiftBundle()

        @JvmStatic
        fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
            INSTANCE.getMessage(key, *params)

        @JvmStatic
        fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> =
            INSTANCE.getLazyMessage(key, *params)
    }
}