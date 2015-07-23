import java.io.* // unused
import java.sql.* // used

import java.util.HashMap // unused
import java.util.ArrayList // used

import java.unresolved.* // unused but unresolved
import java.net.Unresolved // unused but unresolved

import java.net.ConnectException as CE // highlighting of unused aliases not implemented yet

import RootPackageClass // unused because it's in the current package

fun foo(list: ArrayList<String>, p: RootPackageClass) {
    list.add("")
    Date()
}

class RootPackageClass

// WITH_RUNTIME
// FULL_JDK
