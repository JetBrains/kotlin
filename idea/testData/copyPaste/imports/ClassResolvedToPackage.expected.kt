// ERROR: Unresolved reference: a
package to

import a.a

fun f(i: a.a) { // todo must be i: a
    a
    a()
}