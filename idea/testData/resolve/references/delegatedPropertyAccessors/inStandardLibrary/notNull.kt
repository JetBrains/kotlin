import kotlin.properties.Delegates

var x: Int <caret>by Delegates.notNull()

// MULTIRESOLVE
// REF: (in kotlin.properties.ReadWriteProperty).getValue(R,kotlin.PropertyMetadata)
// REF: (in kotlin.properties.ReadWriteProperty).setValue(R,kotlin.PropertyMetadata,T)