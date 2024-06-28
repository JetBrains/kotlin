// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-63984

abstract class ConeClassLikeLookupTag {
    abstract val status: String
}

class ConeClassLikeLookupTagImpl : ConeClassLikeLookupTag() {
    override var status: String = ""
        private set

    fun change(newStatus: String) {
        status = newStatus
    }
}

class ConeClassLikeErrorLookupTag : ConeClassLikeLookupTag() {
    override val status: String
        get() = "ERROR"
}

fun ConeClassLikeLookupTag.foo(): String {
    (this as? ConeClassLikeLookupTagImpl)?.status?.takeIf { it == "OK" }?.let { return it }
    return status.also {
        (this as? ConeClassLikeLookupTagImpl)?.bar(it)
    }
}

fun ConeClassLikeLookupTagImpl.bar(s: String) {
    change(s)
}

fun box(): String {
    val tag = ConeClassLikeErrorLookupTag()
    tag.foo()
    val tag2 = ConeClassLikeLookupTagImpl()
    tag2.change("OK")
    tag2.foo()
    return tag2.status
}