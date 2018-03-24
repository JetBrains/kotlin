data class Test(val notnull: Boolean, val nullable: Boolean?)

fun test(a: Test, b: Test?) {
    <caret>a.notnull == true || a.nullable == true || b?.notnull == true || b?.nullable == true
}