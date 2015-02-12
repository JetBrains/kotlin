// IMPORT: java.util.HashMap
package p

import java.sql.*
import java.util.ArrayList // maybe dropped on adding import with *
import java.util.Date // should not be dropped because of conflicting java.sql.Date class
import java.util.HashSet as JavaHashSet // alias import should not be dropped
import java.util.concurrent // import of package should not be dropped because packages are not imported by *

fun foo() {
    val v1 = JavaHashSet()
    val v2 = Date()
    val v3 = ArrayList()
}