// IS_APPLICABLE: false
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>public</b> operator <b>fun</b> kotlin.String?.plus(other: kotlin.Any?): kotlin.String <i>defined in</i> kotlin</li></ul></html>
fun foo(a: Boolean, b: Boolean) : Boolean {
    return !<caret>(!a + b)
}
