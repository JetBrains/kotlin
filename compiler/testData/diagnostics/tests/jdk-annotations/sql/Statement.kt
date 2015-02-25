fun executeQuery(statement: java.sql.Statement, cmd: String?) {
  statement.executeQuery(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>cmd<!>)
  statement.executeQuery(cmd!!) : java.sql.ResultSet
}

fun executeQuery(statement: java.sql.PreparedStatement) {
  statement.executeQuery() : java.sql.ResultSet
}

fun executeUpdate(statement: java.sql.Statement, cmd: String?) {
  statement.executeUpdate(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>cmd<!>)
  statement.executeUpdate(cmd!!)
}