// !CHECK_TYPE

fun getMetaData(rs: java.sql.ResultSet) {
  checkSubtype<java.sql.ResultSetMetaData>(rs.getMetaData())
}