import java.io.* // unused
import java.sql.* // used

import java.util.HashMap // unused
import java.util.ArrayList // used

import java.unresolved.* // unused but unresolved
import java.net.Unresolved // unused but unresolved

import java.net.ConnectException as CE // highlighting of unused aliases not implemented yet

fun foo(list: ArrayList<String>) {
    list.add("")
    Date()
}

// WITH_RUNTIME
// FULL_JDK
