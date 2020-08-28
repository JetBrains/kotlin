import kotlin.properties.Delegates

var x: Int <caret>by Delegates.notNull()

// MULTIRESOLVE
// REF: (in kotlin.properties.ReadWriteProperty).getValue(T, kotlin.reflect.KProperty<*>)
// REF: (in kotlin.properties.ReadWriteProperty).setValue(T, kotlin.reflect.KProperty<*>, V)
