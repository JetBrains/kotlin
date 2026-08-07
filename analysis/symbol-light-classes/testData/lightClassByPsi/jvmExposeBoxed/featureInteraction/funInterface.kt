// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

// An interface member can never be exposed, so the single abstract method keeps only its mangled form and
// the interface is not implementable from Java.
fun interface Transform {
    fun apply(s: StringWrapper): StringWrapper
}

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
class SamHost(private val transform: Transform) {
    fun run(s: StringWrapper): StringWrapper = transform.apply(s)
}

// Control: a 'fun interface' with no value class in its signature keeps a callable member.
fun interface ControlTransform {
    fun apply(s: String): String
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: Transform.class[apply]
// LIGHT_ELEMENTS_NO_DECLARATION: SamHost.class[run-c5Hft5I], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Transform.class[apply-c5Hft5I]
