// LANGUAGE: +ExplicitBackingFields

annotation class PropertyAnnotation
annotation class FieldAnnotation

@property:PropertyAnnotation
val x: Number
    @FieldAnnotation
    fi<caret>eld: Int = 0
