// "Cast expression 'Boolean' to 'Int'" "false"
// ERROR: Incompatible types: jet.Boolean and jet.Int
fun foo() {
    if (1 is Boolean<caret>);
}
