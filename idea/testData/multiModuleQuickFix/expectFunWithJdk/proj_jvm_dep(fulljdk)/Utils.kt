// "Create expected function in common module proj_Common" "true"
// DISABLE-ERRORS

import java.util.ArrayList

actual fun <T> <caret>createList(): ArrayList<T> = ArrayList()