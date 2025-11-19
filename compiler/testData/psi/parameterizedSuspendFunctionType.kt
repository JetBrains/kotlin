package test

typealias MySuspendAlias <T> = suspend () -> T

val explicitInt: MySuspendAlias<Int>
    get() = null!!

val explicitAny: MySuspendAlias<Any?>
    get() = null!!

val explicitStar: MySuspendAlias<*>
    get() = null!!
