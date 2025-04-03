// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-63246

abstract class Base<T> {
    context(r: R)
    abstract fun <R> String.foo(): Int?

    context(r: R)
    abstract val <R> String.bar: Int?
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Child<!> : Base<String>() {
    context(child: RChild)
    override fun <RChild> String.foo(): Int? = 1

    context(child: RChild)
    <!NOTHING_TO_OVERRIDE!>override<!> val <RChild> String.bar: Int? get() = 1
}
