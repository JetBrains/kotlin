package org.test

@Deprecated("Error", level = DeprecationLevel.ERROR)
@RequiresOptIn
annotation class Error

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
@RequiresOptIn
annotation class Hidden
