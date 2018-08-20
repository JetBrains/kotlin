// "Add '@PropertyTypeMarker' annotation to 'PropertyTypeContainer'" "true"
// COMPILER_ARGUMENTS: -Xuse-experimental=kotlin.Experimental
// WITH_RUNTIME
// ACTION: Add '@PropertyTypeMarker' annotation to containing class 'PropertyTypeContainer'

@Experimental
annotation class PropertyTypeMarker

@PropertyTypeMarker
class PropertyTypeMarked

class PropertyTypeContainer(val subject: Property<caret>TypeMarked)