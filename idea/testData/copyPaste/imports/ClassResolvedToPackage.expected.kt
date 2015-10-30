// ERROR: Unresolved reference: a
// ERROR: Unresolved reference: a
// ERROR: Unresolved reference: a
package to

import a.a

fun f(i: a.a) {
    // TODO References shortening doesn't work for package vs class conflicts under the new resolution rules.
    // After importing 'a.a', expression 'a.a' is unresolved (since 'a' becomes a class).
    // 'package' in expression syntax might be required to fix it properly.
    a.a
    a.a()
}