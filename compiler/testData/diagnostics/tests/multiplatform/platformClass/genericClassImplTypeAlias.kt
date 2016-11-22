// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

platform class C1
platform class C2<A>
platform class C3<B>
platform class C4<D, E>
platform class C5<F, G>
platform class C6<H>
platform class C7<I>
platform class C8<J>
platform class C9<K>
platform class C10<L>

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl typealias C1 = String
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>impl typealias C2<<!UNUSED_TYPEALIAS_PARAMETER!>A<!>> = List<String><!>
<!IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>impl typealias C3<B> = List<B><!>
impl typealias C4<D, E> = MutableMap<D, E>
<!IMPL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION!>impl typealias C5<F, G> = MutableMap<G, F><!>
impl typealias C6<H> = MutableList<H>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C7<I> = MutableList<out I><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C8<<!UNUSED_TYPEALIAS_PARAMETER!>J<!>> = MutableList<*><!>
<!IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>impl typealias C9<K> = MutableList<in K><!>

typealias Tmp<K> = MutableList<K>
<!IMPL_TYPE_ALIAS_NOT_TO_CLASS!>impl typealias C10<L> = Tmp<L><!>
