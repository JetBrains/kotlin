// !CHECK_TYPE

fun executeQuery(statement: java.sql.Statement, cmd: String?) {
  statement.executeQuery(cmd)
  checkSubtype<java.sql.ResultSet>(statement.executeQuery(cmd!!))
}

fun executeQuery(statement: java.sql.PreparedStatement) {
  checkSubtype<java.sql.ResultSet>(statement.executeQuery())
}

fun executeUpdate(statement: java.sql.Statement, cmd: String?) {
  statement.executeUpdate(cmd)
  statement.executeUpdate(cmd!!)
}
