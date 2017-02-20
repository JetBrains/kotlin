package org.jetbrains.kotlin.backend.konan

/**
 * Represents a compilation error caused by mistakes in an input file, e.g. undefined reference.
 */
class KonanCompilationException(message: String = "", cause: Throwable? = null) : Exception(message, cause) {}
