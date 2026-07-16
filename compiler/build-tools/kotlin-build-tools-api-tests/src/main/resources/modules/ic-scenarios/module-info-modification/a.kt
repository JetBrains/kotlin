package foo

import java.sql.SQLException

fun raise(): Nothing = throw SQLException("boom")
