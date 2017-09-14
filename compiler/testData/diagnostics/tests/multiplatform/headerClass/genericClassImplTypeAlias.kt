// !DIAGNOSTICS: -IMPLEMENTATION_WITHOUT_HEADER
// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class C1
expect interface C2<A>
expect interface C3<B>
expect interface C4<D, E>
expect interface C5<F, G>
expect interface C6<H>
expect interface C7<I>
expect interface C8<J>
expect interface C9<K>
expect interface C10<L>

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual typealias C1 = String
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias C2<<!UNUSED_TYPEALIAS_PARAMETER!>A<!>> = List<String><!>
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias C3<B> = List<B><!>
actual typealias C4<D, E> = MutableMap<D, E>
<!IMPL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION!>actual typealias C5<F, G> = MutableMap<G, F><!>
actual typealias C6<H> = MutableList<H>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C7<I> = MutableList<out I><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C8<<!UNUSED_TYPEALIAS_PARAMETER!>J<!>> = MutableList<*><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C9<K> = MutableList<in K><!>

typealias Tmp<K> = MutableList<K>
<!IMPL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias C10<L> = Tmp<L><!>
