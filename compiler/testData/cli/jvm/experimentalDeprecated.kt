package org.test

@Deprecated("Error1", level = DeprecationLevel.ERROR)
@RequiresOptIn
annotation class Error1

@Deprecated("Error2", level = DeprecationLevel.ERROR)
@RequiresOptIn
annotation class Error2

@Deprecated("Hidden1", level = DeprecationLevel.HIDDEN)
@RequiresOptIn
annotation class Hidden1

@Deprecated("Hidden2", level = DeprecationLevel.HIDDEN)
@RequiresOptIn
annotation class Hidden2
