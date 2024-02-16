// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: mustCheckInImports throws kotlin.IllegalStateException instead of java.lang.IllegalStateException
// IGNORE_BACKEND: NATIVE

abstract class Visibility(val name: String, val isPublicAPI: Boolean) {
    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean
}

object Visibilities {
    object Private : Visibility("private", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"

        override fun mustCheckInImports(): Boolean = true
    }

    object Protected : Visibility("protected", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Internal : Visibility("internal", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Public : Visibility("public", isPublicAPI = true) {
        override fun mustCheckInImports(): Boolean = false
    }

    object Local : Visibility("local", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true
    }

    object Inherited : Visibility("inherited", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for INHERITED visibility")
        }
    }

    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean = true

        override val externalDisplayName: String
            get() = "invisible (private in a supertype)"
    }

    object Unknown : Visibility("unknown", isPublicAPI = false) {
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for UNKNOWN visibility")
        }
    }

    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0)
        put(Internal, 1)
        put(Protected, 1)
        put(Public, 2)
    }
}
