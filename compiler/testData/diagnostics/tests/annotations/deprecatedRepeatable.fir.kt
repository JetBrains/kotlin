// FULL_JDK

import java.lang.annotation.Repeatable

@java.lang.annotation.Repeatable(Annotations::class) annotation class RepAnn

@Repeatable(OtherAnnotations::class) annotation class OtherAnn

annotation class Annotations(vararg val value: RepAnn)

annotation class OtherAnnotations(vararg val value: OtherAnn)
