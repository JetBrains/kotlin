// "Remove explicitly specified function return type" "true"
// ERROR: Function declaration must have a name
fun (): Int {
    return<caret>
}