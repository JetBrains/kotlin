// FIR_IDENTICAL
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

@field:BadA
val fieldedBadName: String = "badField"

@field:GoodA
val fieldedGoodName: String = "goodField"
