// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE

// MODULE: a
// FILE: A.kt

//class FirstClass : SecondClass()
//
//open class SecondClass

class ConfigurationTarget(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@ConfigField<!> val target: String)

@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigField()

// MODULE: b
// FILE: B.kt

@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigField()

class ConfigurationTarget(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@ConfigField<!> val target: String)
