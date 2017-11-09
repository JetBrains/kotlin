// IMPORT: java.util.Calendar
package p

import java.sql.*
import java.util.Properties // maybe dropped on adding import with *
import java.util.Date // should not be dropped because of conflicting java.sql.Date class
import java.util.HashSet as JavaHashSet // alias import should not be dropped

fun foo() {
    val v1 = JavaHashSet()
    val v2 = Date()
    val v3 = Properties()
}