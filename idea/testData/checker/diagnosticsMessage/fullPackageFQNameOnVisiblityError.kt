// WITH_RUNTIME

package a.b

@<error descr="[INVISIBLE_MEMBER] Cannot access 'InlineOnly': it is internal in 'kotlin.internal'">kotlin.internal.<error descr="[INVISIBLE_REFERENCE] Cannot access 'InlineOnly': it is internal in 'kotlin.internal'">InlineOnly</error></error>
inline fun foo() {}