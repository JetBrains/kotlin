package test

typealias MySuspendAlias <T> = suspend (Int, suspend (String) -> T) -> T

val explicitInt: MySuspendAlias<Int>
    get() = null!!

val explicitAny: MySuspendAlias<Any?>
    get() = null!!
