package foo

import java.sql.SQLException

fun throwException(): Nothing = throw SQLException("boom")
