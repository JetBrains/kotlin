// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// RENDER_DIAGNOSTICS_FULL_TEXT
// WITH_STDLIB

typealias DeprecatedTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.Deprecated<!>
typealias TargetTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.annotation.Target<!>
typealias TargetJavaTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>java.lang.annotation.Target<!>
typealias JvmNameTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.jvm.JvmName<!>
typealias DeprecatedSinceKotlinTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.DeprecatedSinceKotlin<!>
typealias SinceKotlinTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.SinceKotlin<!>
typealias JavaDeprecatedTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>java.lang.Deprecated<!>
typealias JvmRecordTA = <!TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION_ERROR!>kotlin.jvm.JvmRecord<!>

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
