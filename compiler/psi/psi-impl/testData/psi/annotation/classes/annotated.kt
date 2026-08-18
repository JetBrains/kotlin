// An annotation of a constructor parameter lands on the parameter, the property or the getter,
// and the metadata keeps each of them apart
package test

annotation class Marker

annotation class Targeted(
    @Marker val onParameter: Int,
    @property:Marker val onProperty: Int,
    @get:Marker val onGetter: Int,
    @param:Marker @property:Marker @get:Marker val onEveryTarget: Int,
)
