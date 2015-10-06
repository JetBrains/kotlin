// "Move type parameter constraint to 'where' clause" "true"
fun <<caret>T : Cloneable> foo() where T : Comparable<*> {
}
