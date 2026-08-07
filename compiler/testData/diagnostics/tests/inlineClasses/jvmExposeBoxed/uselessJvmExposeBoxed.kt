// RUN_PIPELINE_TILL: BACKEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

// A value class that only appears as a generic argument is erased, so nothing is boxed and nothing is mangled.
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
fun genericArgumentOnly(l: List<IC>) {}

// An extension function on a value class receiver is mangled, so exposing it does have an effect.
@JvmExposeBoxed
fun IC.extensionFunction(): Int = s.length

// An extension property is mangled the same way and a boxed 'getExtensionProperty(IC)' is emitted, so no
// diagnostic belongs here either. 'FirJvmExposeBoxedChecker.canBeOverloadedByExposed' reads the receiver off
// the accessor instead of going through 'propertyIfAccessor', so it does not see the value class receiver.
// TODO: Remove if green after the fix
@get:JvmExposeBoxed
val IC.extensionProperty: Int
    get() = s.length

// 'internal' is not private API, so nothing is reported.
@JvmExposeBoxed
internal fun internalFunction(ic: IC): IC = ic

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, annotationUseSiteTargetPropertyGetter, classDeclaration,
classReference, funWithExtensionReceiver, functionDeclaration, getter, primaryConstructor, propertyDeclaration,
propertyWithExtensionReceiver, value */
