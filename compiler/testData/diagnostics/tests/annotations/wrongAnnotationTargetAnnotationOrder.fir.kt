// ISSUE: KT-68489

// FILE: BadA.java
@java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
public @interface BadA {}

// FILE: GoodA.java
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
public @interface GoodA {}

// FILE: Main.kt
interface Test {
    @BadA
    val badName: String

    @GoodA
    val goodName: String
}

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:BadA<!>
val fieldedBadName: String = "badField"

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:GoodA<!>
val fieldedGoodName: String = "goodField"
