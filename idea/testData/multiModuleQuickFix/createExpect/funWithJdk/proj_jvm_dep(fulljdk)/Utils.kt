// "Create expected function in common module proj_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,fun createList() = ArrayList()
// DISABLE-ERRORS

import java.util.ArrayList

actual fun <T> <caret>createList(): ArrayList<T> = ArrayList()