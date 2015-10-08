// "Move type parameter constraint to 'where' clause" "true"
fun foo<<caret>T : Cloneable> where T : Comparable<*> {
}
