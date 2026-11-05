// METADATA_TARGET_PLATFORMS: JS, WasmJs
// LANGUAGE: +MultiPlatformProjects
// LANGUAGE: -ExplicitInlineValueClassesSyntaxRequiredOnAllPlatforms
// WITH_STDLIB

import kotlin.jvm.JvmInline

@JvmInline
value class A(val x: Int)

@JvmInline
value class B<!INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE!>(val x: Int, val y: Int)<!>

<!JVM_INLINE_WITHOUT_VALUE_CLASS_WARNING!>@JvmInline<!>
class C(val x: Int)

<!JVM_INLINE_WITHOUT_VALUE_CLASS_WARNING!>@JvmInline<!>
object D

<!JVM_INLINE_WITHOUT_VALUE_CLASS_WARNING!>@JvmInline<!>
interface E
