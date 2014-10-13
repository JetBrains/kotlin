fun executeQuery(statement: java.sql.Statement, cmd: String?) {
  statement.executeQuery(cmd)
  statement.executeQuery(cmd!!) : java.sql.ResultSet
}

fun executeQuery(statement: java.sql.PreparedStatement) {
  statement.executeQuery() : java.sql.ResultSet
}

fun executeUpdate(statement: java.sql.Statement, cmd: String?) {
  statement.executeUpdate(cmd)
  statement.executeUpdate(cmd!!)
}