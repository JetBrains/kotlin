// The direct invoke optimization silently broke several tests. This test exists to
// ensure that we don't end up optimizing the indirect invokes that are present in the tests.
private fun id(x: String) = x

fun box() = { "O" }.let { it() } + ::id.let { it("K") }

// CHECK_BYTECODE_TEXT
// 2 Function[^.\n]*\.invoke
