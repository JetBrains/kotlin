sealed class Sealed

sealed class SealedWithParameter(val x: Int)

sealed class SealedPrivateConstructor private constructor(val x: Int)

sealed class SealedProtectedConstructor protected constructor(val x: Int)

sealed interface SealedInterface

enum class Enum {
    FIRST
}

enum class EnumPrivateConstructor private constructor(val x: Int) {
    FIRST(1)
}

object Object

class WithCompanion {
    companion object
}

class InternalConstructor internal constructor(val x: Int)

class PrivateConstructor private constructor(val x: Int)

open class ProtectedConstructor protected constructor(val x: Int)

class PublicConstructor(val x: Int)
