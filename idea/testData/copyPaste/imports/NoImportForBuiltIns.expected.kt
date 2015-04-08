// ERROR: Cannot access 'd': it is 'private' in 'a'
// ERROR: Cannot access 'd': it is 'private' in 'a'
// ERROR: Cannot access 'b': it is 'private' in 'a'
// ERROR: Cannot access 'b': it is 'private' in 'a'
package to

import a.b
import a.d

fun f(c: IntRange) = d + b