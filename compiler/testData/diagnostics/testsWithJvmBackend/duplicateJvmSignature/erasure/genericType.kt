// !DIAGNOSTICS: -UNUSED_PARAMETER

class G<T>

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: G<String>): G<Int><!> {null!!}
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: G<Int>): G<String><!> {null!!}