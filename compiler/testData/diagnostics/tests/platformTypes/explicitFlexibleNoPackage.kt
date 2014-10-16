// !DIAGNOSTICS: -UNUSED_PARAMETER
// !EXPLICIT_FLEXIBLE_TYPES
// !CHECK_TYPE

fun foo(f: ft<Int, Int?>) {
    f.checkType { it : _<Int> }
    f.checkType { it : _<Int?> }
}