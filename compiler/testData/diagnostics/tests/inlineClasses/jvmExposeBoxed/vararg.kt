// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmInline
value class IC(val s: String)

// 'vararg counts: UInt' has parameter type 'UIntArray', which is itself a '@JvmInline value class', so the
// declaration is mangled and exposing it has an effect.
@JvmExposeBoxed
fun unsignedVararg(vararg counts: UInt) {}

@JvmExposeBoxed
fun unsignedVarargReturn(vararg counts: UInt): UInt = counts.size.toUInt()

// A vararg of an ordinary type erases to an array, so on its own there is nothing to box.
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
fun ordinaryVararg(vararg parts: String) {}

// Here the mangling comes from the value class parameter, not from the vararg.
@JvmExposeBoxed
fun varargWithValueClass(vararg parts: String, ic: IC) {}

// A user value class has no corresponding array type, so a vararg of it is rejected by Kotlin itself.
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
fun userValueClassVararg(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> ics: IC) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, value, vararg */
