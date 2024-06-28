// RESOLVE_SCRIPT
// BODY_RESOLVE
// MEMBER_NAME_FILTER: $$result

package foo

annotation class Anno(val position: String)
const val constant = 0

fun foo(action: () -> Unit) {}

@Anno("call $constant")
foo {
    @Anno("property $constant")
    val i: @Anno("local type $constant") Int
}
