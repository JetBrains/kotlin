// "Replace usages of 'oldProp: String' in whole project" "true"

import pack.oldProp
import pack.foo
import pack.newProp

fun foo() {
    foo(<caret>newProp)
}
