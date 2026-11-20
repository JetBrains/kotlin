// ISSUE: KT-91111
// LANGUAGE: -IrRichCallableReferencesInKlibs
// WITH_STDLIB
// KLIBs built by compilers up to 2.2 store the property reference of the delegate as a legacy callable reference
// of type `KProperty1<Holder<*>, String>`. The language feature is disabled to make the 2.3.0 first-stage compiler do the same.
// When such a KLIB is consumed, `UpgradeCallableReferences` wraps the getter and builds a type substitutor
// from the star-projected type of the anonymous object, which is declared in a member of a generic class here.

class Holder<T>(val value: T) {
    fun get(): String {
        val o = object {
            val lazyValue: String by lazy { value.toString() }
        }
        return o.lazyValue
    }
}

fun box(): String = Holder("OK").get()
