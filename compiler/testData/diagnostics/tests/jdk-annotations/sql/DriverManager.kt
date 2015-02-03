import java.sql.DriverManager

fun getConnection(url: String?) {
  DriverManager.getConnection(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>url<!>)
  DriverManager.getConnection(url!!) : java.sql.Connection
}

fun getConnection(url: String?, props: java.util.Properties?) {
  DriverManager.getConnection(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>url<!>, props)
  DriverManager.getConnection(url!!, props) : java.sql.Connection
}

fun getConnection(url: String?, user: String?, password: String?) {
  DriverManager.getConnection(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>url<!>, user!!, password!!)
  DriverManager.getConnection(url!!, user, password<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
  DriverManager.getConnection(url<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>, user<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>, password)
  DriverManager.getConnection(url<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>, user<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>, password<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) : java.sql.Connection
}

fun getDriver(url: String?) {
  DriverManager.getDriver(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>url<!>)
  DriverManager.getDriver(url!!) : java.sql.Driver
}

fun registerDriver(driver: java.sql.Driver?) {
   DriverManager.registerDriver(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>driver<!>)
   DriverManager.registerDriver(driver!!)
}

fun getDrivers() {
   // todo fix to java.util.Enumeration<java.sql.Driver> bug in compiler fixed
   DriverManager.getDrivers() : java.util.Enumeration<*>
}