// "Create expected function in common module proj_Common" "true"
// SHOULD_FAIL_WITH: Cannot generate expected function: Type java.util.ArrayList<kotlin.Any> is not accessible from common code
// DISABLE-ERRORS

import java.util.ArrayList

actual fun <T> <caret>createList(): ArrayList<T> = ArrayList()