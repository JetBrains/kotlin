// FILE: lib.kt
package one

@RequiresOptIn("")
private annotation class PrivateOptInMarker

private annotation class PrivateAnnotation

// FILE: usage.kt
package one

@PrivateOptInMarker
fun optInMarkerUsage() {}

@PrivateAnnotation
fun annotationUsage() {}

@OptIn(PrivateOptInMarker::class)
fun optInArgumentUsage() {}

@SubclassOptInRequired(PrivateOptInMarker::class)
open class SubclassOptInArgumentUsage
