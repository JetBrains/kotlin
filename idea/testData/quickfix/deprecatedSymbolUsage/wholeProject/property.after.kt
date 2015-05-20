// "Replace usages of 'oldProp: String' in whole project" "true"

import pack.*

fun foo() {
    foo(<caret>newProp)
}
