// !DIAGNOSTICS: -UNUSED_PARAMETER

class G<T>

<!CONFLICTING_PLATFORM_DECLARATIONS!>fun foo(x: G<String>): G<Int> {null!!}<!>
<!CONFLICTING_PLATFORM_DECLARATIONS!>fun foo(x: G<Int>): G<String> {null!!}<!>