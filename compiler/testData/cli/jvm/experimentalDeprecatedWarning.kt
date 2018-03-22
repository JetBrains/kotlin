package org.test

@Deprecated("BinaryWarning", level = DeprecationLevel.WARNING)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.RUNTIME])
annotation class BinaryWarning

@Deprecated("SourceWarning", level = DeprecationLevel.WARNING)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
annotation class SourceWarning
