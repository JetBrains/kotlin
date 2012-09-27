fun executeQuery(statement: java.sql.Statement, cmd: String?) {
  statement.executeQuery(<!TYPE_MISMATCH!>cmd<!>)
  statement.executeQuery(cmd!!) : java.sql.ResultSet
}

fun executeQuery(statement: java.sql.PreparedStatement) {
  statement.executeQuery() : java.sql.ResultSet
}

fun executeUpdate(statement: java.sql.Statement, cmd: String?) {
  statement.<!NONE_APPLICABLE!>executeUpdate<!>(cmd)
  statement.executeUpdate(cmd!!)
}