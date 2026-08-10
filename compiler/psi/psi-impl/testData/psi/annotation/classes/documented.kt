// LANGUAGE: +ExportKDocDocumentationToKlib
// The KDoc of a parameter is stored on its property, which only a klib keeps
package test

annotation class Documented(
    /**
     * The documented value.
     */
    val value: Int = 1,
)
