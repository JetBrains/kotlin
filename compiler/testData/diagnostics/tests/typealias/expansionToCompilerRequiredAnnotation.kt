// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_IDENTICAL
// WITH_STDLIB

typealias DeprecatedTA = kotlin.Deprecated
typealias TargetTA = kotlin.annotation.Target
typealias TargetJavaTA = java.lang.annotation.Target
typealias JvmNameTA = kotlin.jvm.JvmName
typealias DeprecatedSinceKotlinTA = kotlin.DeprecatedSinceKotlin
typealias SinceKotlinTA = kotlin.SinceKotlin
typealias JavaDeprecatedTA = java.lang.Deprecated
typealias JvmRecordTA = kotlin.jvm.JvmRecord

// The following annotations affects compilation but backend part, so they can't break resolving.
// Treat them as allowed for now, but later they also can be forbidden if necessary or for unification.
typealias JvmOverloadsTA = kotlin.jvm.JvmOverloads
typealias JvmStaticTA = kotlin.jvm.JvmStatic
typealias JvmMultifileClassTA = kotlin.jvm.JvmMultifileClass
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>JvmPackageNameTA<!> = kotlin.jvm.<!INVISIBLE_REFERENCE!>JvmPackageName<!>
typealias JvmSyntheticTA = kotlin.jvm.JvmSynthetic
typealias ThrowsTA = kotlin.jvm.Throws
typealias JvmFieldTA = kotlin.jvm.JvmField
typealias JvmSuppressWildcardsTA = kotlin.jvm.JvmSuppressWildcards
typealias JvmWildcardTA = kotlin.jvm.JvmWildcard
typealias JvmInlineTA = kotlin.jvm.JvmInline
@OptIn(ExperimentalStdlibApi::class)
typealias JvmExposeBoxedTA = kotlin.jvm.JvmExposeBoxed

/* GENERATED_FIR_TAGS: typeAliasDeclaration */
