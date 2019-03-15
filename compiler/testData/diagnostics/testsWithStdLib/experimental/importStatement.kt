// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package feature.experimental.self

@Experimental
annotation class ImportedMarker

@ImportedMarker
object ImportedClass {
    @ImportedMarker
    fun importedObjectMember() {}
}

@ImportedMarker
fun importedFunction() {}

@ImportedMarker
val importedProperty = Unit

// FILE: usage.kt

import feature.experimental.self.ImportedMarker
import feature.experimental.self.ImportedClass
import feature.experimental.self.importedFunction
import feature.experimental.self.importedProperty
import feature.experimental.self.ImportedClass.importedObjectMember
