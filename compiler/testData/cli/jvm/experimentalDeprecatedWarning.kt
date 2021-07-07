package org.test

@Deprecated("Warning1", level = DeprecationLevel.WARNING)
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Warning1

@Deprecated("Warning2", level = DeprecationLevel.WARNING)
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class Warning2

@RequiresOptIn
annotation class OneMore
