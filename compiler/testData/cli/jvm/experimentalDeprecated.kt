package org.test

@Deprecated("BinaryError", level = DeprecationLevel.ERROR)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.RUNTIME])
annotation class BinaryError

@Deprecated("BinaryHidden", level = DeprecationLevel.HIDDEN)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.RUNTIME])
annotation class BinaryHidden

@Deprecated("SourceError", level = DeprecationLevel.ERROR)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
annotation class SourceError

@Deprecated("SourceHidden", level = DeprecationLevel.HIDDEN)
@Experimental(Experimental.Level.ERROR, [Experimental.Impact.COMPILATION])
annotation class SourceHidden
