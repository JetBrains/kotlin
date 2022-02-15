// !DIAGNOSTICS: -ACTUAL_WITHOUT_EXPECT
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

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias C1 = String
<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias C2<A> = List<String><!>
<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias C3<B> = List<B><!>
actual typealias C4<D, E> = MutableMap<D, E>
<!ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION!>actual typealias C5<F, G> = MutableMap<G, F><!>
actual typealias C6<H> = MutableList<H>
<!ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C7<I> = MutableList<out I><!>
<!ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C8<J> = MutableList<*><!>
<!ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE!>actual typealias C9<K> = MutableList<in K><!>

typealias Tmp<K> = MutableList<K>
<!ACTUAL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias C10<L> = Tmp<L><!>
