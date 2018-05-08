package org.test

@Deprecated("Error1", level = DeprecationLevel.ERROR)
@Experimental
annotation class Error1

@Deprecated("Error2", level = DeprecationLevel.ERROR)
@Experimental
annotation class Error2

@Deprecated("Hidden1", level = DeprecationLevel.HIDDEN)
@Experimental
annotation class Hidden1

@Deprecated("Hidden2", level = DeprecationLevel.HIDDEN)
@Experimental
annotation class Hidden2
