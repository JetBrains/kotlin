package org.test

@Deprecated("Warning", level = DeprecationLevel.WARNING)
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Warning

@RequiresOptIn
annotation class OneMore
