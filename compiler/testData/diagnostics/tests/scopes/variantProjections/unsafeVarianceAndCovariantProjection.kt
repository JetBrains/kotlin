// FIR_IDENTICAL
// ISSUE: KT-54764

interface Out1<out T> {
    fun copy(t: @UnsafeVariance T): Out1<T>
}

fun foo1(o1: Out1<*>, o2: Out1<<!REDUNDANT_PROJECTION!>out<!> Any?>, o3: Out1<Any?>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out1<kotlin.Any?>")!>o1.copy("")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out1<kotlin.Any?>")!>o2.copy("")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out1<kotlin.Any?>")!>o3.copy("")<!>
}

interface Out2<out T : CharSequence> {
    fun copy(t: @UnsafeVariance T): Out2<T>
}

fun foo2(o1: Out2<*>, o2: Out2<<!REDUNDANT_PROJECTION!>out<!> CharSequence>, o3: Out2<CharSequence>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out2<kotlin.CharSequence>")!>o1.copy("")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out2<kotlin.CharSequence>")!>o2.copy("")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out2<kotlin.CharSequence>")!>o3.copy("")<!>
}

interface Out3<out T : Out3<T>> {
    fun copy(t: @UnsafeVariance T): Out3<T>
}

fun foo3(o1: Out3<*>, o2: Out3<<!REDUNDANT_PROJECTION!>out<!> Out3<*>>, o3: Out3<Out3<*>>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out3<Out3<*>>")!>o1.copy(o1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out3<Out3<*>>")!>o2.copy(o1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out3<Out3<*>>")!>o3.copy(o1)<!>
}
