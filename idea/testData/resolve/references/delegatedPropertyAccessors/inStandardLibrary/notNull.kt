import kotlin.properties.Delegates

var x: Int <caret>by Delegates.notNull()

// MULTIRESOLVE
// REF: (in kotlin.properties.ReadWriteProperty).get(R,jet.PropertyMetadata)
// REF: (in kotlin.properties.ReadWriteProperty).set(R,jet.PropertyMetadata,T)