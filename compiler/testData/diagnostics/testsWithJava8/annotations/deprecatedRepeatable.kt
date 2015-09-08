import java.lang.annotation.Repeatable

<!DEPRECATED_JAVA_ANNOTATION!>@java.lang.annotation.Repeatable(Annotations::class)<!> annotation class RepAnn

<!DEPRECATED_JAVA_ANNOTATION!>@Repeatable(OtherAnnotations::class)<!> annotation class OtherAnn

annotation class Annotations(vararg val value: RepAnn)

annotation class OtherAnnotations(vararg val value: OtherAnn)
