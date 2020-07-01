package com.jetbrains.mobile

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_PATH = "messages.MobileBundle"

object MobileBundle : AbstractBundle(BUNDLE_PATH) {
    fun message(@PropertyKey(resourceBundle = BUNDLE_PATH) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}