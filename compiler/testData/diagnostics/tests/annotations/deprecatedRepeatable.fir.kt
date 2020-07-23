// FULL_JDK

import java.lang.annotation.Repeatable

<!INAPPLICABLE_CANDIDATE!>@java.lang.annotation.Repeatable(Annotations::class)<!> annotation class RepAnn

<!INAPPLICABLE_CANDIDATE!>@Repeatable(OtherAnnotations::class)<!> annotation class OtherAnn

annotation class Annotations(vararg val value: RepAnn)

annotation class OtherAnnotations(vararg val value: OtherAnn)
