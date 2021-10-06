// FILE: test.kt
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR)
annotation class KotlinMessage

data class KotlinResult @KotlinMessage constructor(@get:KotlinMessage <!REPEATED_ANNOTATION!>@KotlinMessage<!> val message: String = "")

open class Some {
    companion object {
        <!INAPPLICABLE_JVM_NAME!>@get:JvmName("getInstance")<!>
        <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> val INSTANCE: String
            get() = "Omega"
    }
}
