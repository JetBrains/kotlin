import java.sql.DriverManager

fun getConnection(url: String?) {
  DriverManager.<!NONE_APPLICABLE!>getConnection<!>(url)
  DriverManager.getConnection(url!!) : java.sql.Connection
}

fun getConnection(url: String?, props: java.util.Properties?) {
  DriverManager.<!NONE_APPLICABLE!>getConnection<!>(url, props)
  DriverManager.getConnection(url!!, props) : java.sql.Connection
}

fun getConnection(url: String?, user: String?, password: String?) {
  DriverManager.<!NONE_APPLICABLE!>getConnection<!>(url, user!!, password!!)
  DriverManager.<!NONE_APPLICABLE!>getConnection<!>(url!!, user, password!!)
  DriverManager.<!NONE_APPLICABLE!>getConnection<!>(url!!, user!!, password)
  DriverManager.getConnection(url!!, user!!, password!!) : java.sql.Connection
}

fun getDriver(url: String?) {
  DriverManager.getDriver(<!TYPE_MISMATCH!>url<!>)
  DriverManager.getDriver(url!!) : java.sql.Driver
}

fun registerDriver(driver: java.sql.Driver?) {
   DriverManager.registerDriver(<!TYPE_MISMATCH!>driver<!>)
   DriverManager.registerDriver(driver!!)
}

fun getDrivers() {
   // todo fix to java.util.Enumeration<java.sql.Driver> bug in compiler fixed
   DriverManager.getDrivers() : java.util.Enumeration<*>
}